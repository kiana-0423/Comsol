package com.nfm.comsol;

import com.comsol.model.util.ModelUtil;
import com.nfm.comsol.config.ConfigLoader;
import com.nfm.comsol.config.MaterialConfig;
import com.nfm.comsol.config.SimulationConfig;
import com.nfm.comsol.config.FullCellConfig;
import com.nfm.comsol.fullcell.FullCellBatchRunner;
import com.nfm.comsol.fullcell.FullCellSimulationRunner;
import com.nfm.comsol.fullcell.SensitivityCase;
import com.nfm.comsol.runner.BatchRunner;
import com.nfm.comsol.runner.SimulationRunner;
import com.nfm.comsol.util.PathUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** CLI entry point only: parsing and dispatch live here; model construction does not. */
public final class Main {
    private Main() {}

    public static void main(String[] rawArgs) {
        int exit = 0;
        try {
            String[] args = effectiveArgs(rawArgs);
            Cli cli = Cli.parse(args);
            Path root = PathUtils.detectProjectRoot();
            Path simulationFile = cli.config == null ? Path.of("config", "simulation.properties") : Path.of(cli.config);
            SimulationConfig simulation = ConfigLoader.loadSimulation(root, simulationFile);
            String mode = cli.mode == null
                    ? (cli.model.equals("full-cell") ? "cycle" : simulation.defaultMode())
                    : cli.mode;

            if (!ModelUtil.getComsolVersion().startsWith("6.4")) {
                throw new IllegalStateException("COMSOL 6.4 required, found " + ModelUtil.getComsolVersion());
            }

            if (cli.model.equals("full-cell")) {
                if (cli.exportOnly) throw new IllegalArgumentException("--export-only is not yet supported for --model full-cell");
                FullCellConfig cell = ConfigLoader.loadFullCell(root,
                        cli.fullCellConfig == null ? Path.of("config", "full_cell.properties") : Path.of(cli.fullCellConfig));
                FullCellBatchRunner batch = new FullCellBatchRunner();
                var options = new FullCellSimulationRunner.RunOptions(cli.cRate, mode, cli.buildOnly, cli.smokeTest);
                if (cli.all) {
                    System.out.println("Full-cell summary: " + batch.runAll(cell, simulation, options));
                } else {
                    MaterialConfig material = ConfigLoader.loadMaterial(root, cli.material);
                    if (cli.meshConvergence) {
                        System.out.println("Full-cell mesh convergence: "
                                + batch.runMeshConvergence(material, cell, simulation, cli.cRate, mode));
                    } else if (cli.timeConvergence) {
                        System.out.println("Full-cell time convergence: "
                                + batch.runTimeConvergence(material, cell, simulation, cli.cRate, mode));
                    } else if (cli.parameterSensitivity) {
                        System.out.println("Full-cell sensitivity: "
                                + batch.runSensitivity(material, cell, simulation, cli.cRate, mode));
                    } else if (cli.parameterAttribution) {
                        System.out.println("Full-cell parameter attribution: "
                                + batch.runAttribution(cell, simulation, cli.cRate, mode));
                    } else {
                        new FullCellSimulationRunner().run(material, cell, simulation, options,
                                SensitivityCase.baseline());
                    }
                }
                return;
            }

            SimulationRunner.RunOptions options = new SimulationRunner.RunOptions(
                    cli.cRate, mode, cli.buildOnly, cli.exportOnly, cli.smokeTest);
            BatchRunner batch = new BatchRunner();
            if (cli.all) {
                batch.runAll(simulation, options);
            } else {
                MaterialConfig material = ConfigLoader.loadMaterial(root, cli.material);
                if (cli.meshConvergence) {
                    Path output = batch.runMeshConvergence(material, simulation, cli.cRate, mode);
                    System.out.println("Mesh convergence table: " + output);
                } else {
                    new SimulationRunner().run(material, simulation, options);
                }
            }
        } catch (Exception ex) {
            exit = 1;
            ex.printStackTrace(System.err);
        } finally {
            try { ModelUtil.clear(); } catch (Exception ignored) { /* Initialization may have failed. */ }
        }
        if (exit != 0) System.exit(exit);
    }

    /** COMSOL batch owns its command line, so platform launch scripts pass application arguments via this variable. */
    private static String[] effectiveArgs(String[] raw) {
        if (raw != null && raw.length > 0) return raw;
        String env = System.getenv("NFM_COMSOL_ARGS");
        if (env == null || env.isBlank()) return new String[0];
        return splitCommandLine(env).toArray(String[]::new);
    }

    private static List<String> splitCommandLine(String text) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') quoted = !quoted;
            else if (Character.isWhitespace(c) && !quoted) {
                if (!current.isEmpty()) { tokens.add(current.toString()); current.setLength(0); }
            } else current.append(c);
        }
        if (quoted) throw new IllegalArgumentException("Unclosed quote in NFM_COMSOL_ARGS");
        if (!current.isEmpty()) tokens.add(current.toString());
        return tokens;
    }

    private static final class Cli {
        String material = "NFM";
        String model = "particle";
        double cRate = 1.0;
        String mode;
        String config;
        String fullCellConfig;
        boolean all;
        boolean buildOnly;
        boolean exportOnly;
        boolean smokeTest;
        boolean meshConvergence;
        boolean parameterSensitivity;
        boolean parameterAttribution;
        boolean timeConvergence;

        static Cli parse(String[] args) {
            Cli c = new Cli();
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--model" -> c.model = value(args, ++i, "--model").toLowerCase();
                    case "--material" -> c.material = value(args, ++i, "--material");
                    case "--c-rate" -> c.cRate = Double.parseDouble(value(args, ++i, "--c-rate"));
                    case "--mode" -> c.mode = value(args, ++i, "--mode").toLowerCase();
                    case "--config" -> c.config = value(args, ++i, "--config");
                    case "--full-cell-config" -> c.fullCellConfig = value(args, ++i, "--full-cell-config");
                    case "--all" -> c.all = true;
                    case "--build-only" -> c.buildOnly = true;
                    case "--solve" -> { /* solve is the default explicit action */ }
                    case "--export-only" -> c.exportOnly = true;
                    case "--smoke-test" -> c.smokeTest = true;
                    case "--mesh-convergence" -> c.meshConvergence = true;
                    case "--parameter-sensitivity" -> c.parameterSensitivity = true;
                    case "--parameter-attribution" -> c.parameterAttribution = true;
                    case "--time-convergence" -> c.timeConvergence = true;
                    case "--help", "-h" -> { usage(); System.exit(0); }
                    default -> throw new IllegalArgumentException("Unknown argument: " + args[i]);
                }
            }
            if (c.buildOnly && c.exportOnly) throw new IllegalArgumentException("--build-only and --export-only are mutually exclusive");
            if (!c.model.equals("particle") && !c.model.equals("full-cell")) {
                throw new IllegalArgumentException("--model must be particle or full-cell");
            }
            if (c.model.equals("full-cell") && Math.abs(c.cRate - 0.1) > 1e-9
                    && Math.abs(c.cRate - 1.0) > 1e-9) {
                throw new IllegalArgumentException("--model full-cell supports --c-rate 0.1 or 1");
            }
            if (c.model.equals("particle") && c.mode != null && c.mode.equals("cycle")) {
                throw new IllegalArgumentException("--mode cycle is available only for --model full-cell");
            }
            if (c.mode != null && !List.of("charge", "discharge", "cycle").contains(c.mode)) {
                throw new IllegalArgumentException("--mode must be charge, discharge, or cycle");
            }
            if (c.model.equals("particle") && (c.parameterSensitivity || c.parameterAttribution || c.timeConvergence)) {
                throw new IllegalArgumentException("sensitivity, attribution, and time convergence require --model full-cell");
            }
            if (c.meshConvergence && (c.all || c.buildOnly || c.exportOnly || c.smokeTest || c.timeConvergence)) {
                throw new IllegalArgumentException("--mesh-convergence cannot be combined with --all/build-only/export-only/smoke-test");
            }
            if (c.timeConvergence && (c.all || c.buildOnly || c.exportOnly || c.smokeTest || c.parameterSensitivity)) {
                throw new IllegalArgumentException("--time-convergence cannot be combined with all/build/export/smoke/sensitivity");
            }
            if (c.parameterSensitivity && (c.all || c.buildOnly || c.exportOnly || c.smokeTest || c.meshConvergence || c.timeConvergence || c.parameterAttribution)) {
                throw new IllegalArgumentException("--parameter-sensitivity cannot be combined with all/build/export/smoke/mesh convergence");
            }
            if (c.parameterAttribution && (c.all || c.buildOnly || c.exportOnly || c.smokeTest
                    || c.meshConvergence || c.timeConvergence)) {
                throw new IllegalArgumentException("--parameter-attribution cannot be combined with batch/build/export/smoke/convergence");
            }
            if (c.smokeTest) { c.material = "NFM"; c.cRate = 1.0; c.mode = "charge"; }
            return c;
        }

        private static String value(String[] args, int index, String option) {
            if (index >= args.length) throw new IllegalArgumentException("Missing value after " + option);
            return args[index];
        }

        private static void usage() {
            System.out.println("COMSOL Na particle model (Linux/macOS, COMSOL 6.4)\n"
                    + "  --model particle|full-cell --material NFM|NFMZC --c-rate 0.1|1\n"
                    + "  --mode charge|discharge|cycle\n"
                    + "  --all | --build-only | --solve | --export-only | --smoke-test\n"
                    + "  --mesh-convergence | --time-convergence | --parameter-sensitivity | --parameter-attribution\n"
                    + "  --config config/simulation.properties --full-cell-config config/full_cell.properties");
        }
    }
}
