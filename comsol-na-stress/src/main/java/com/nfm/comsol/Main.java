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
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
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
            Path simulationFile = cli.config == null
                    ? Paths.get("config", "simulation.properties") : Paths.get(cli.config);
            SimulationConfig simulation = ConfigLoader.loadSimulation(root, simulationFile);
            String mode = cli.mode == null
                    ? (cli.model.equals("full-cell") ? "cycle" : simulation.defaultMode())
                    : cli.mode;

            String comsolVersion = ModelUtil.getComsolVersion();
            if (!comsolVersion.matches(".*(^|[^0-9])6\\.4([^0-9]|$).*")) {
                throw new IllegalStateException("COMSOL 6.4 required, found " + comsolVersion);
            }

            if (cli.model.equals("full-cell")) {
                FullCellConfig cell = ConfigLoader.loadFullCell(root,
                        cli.fullCellConfig == null
                                ? Paths.get("config", "full_cell.properties") : Paths.get(cli.fullCellConfig));
                FullCellBatchRunner batch = new FullCellBatchRunner();
                FullCellSimulationRunner.RunOptions options =
                        new FullCellSimulationRunner.RunOptions(
                                cli.cRate, mode, cli.buildOnly, cli.exportOnly,
                                cli.smokeTest, cli.transitionSmokeTest);
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

    /**
     * COMSOL batch owns its command line and its security manager blocks getenv.
     * The Unix launcher therefore writes one application argument per line.
     */
    private static String[] effectiveArgs(String[] raw) {
        if (raw != null && raw.length > 0) return raw;
        Path argsFile = Paths.get("target", "nfm_comsol_args.txt");
        if (!Files.isRegularFile(argsFile)) return new String[0];
        try {
            List<String> lines = Files.readAllLines(argsFile);
            List<String> args = new ArrayList<>();
            for (String line : lines) {
                if (!line.isEmpty()) args.add(line);
            }
            return args.toArray(new String[0]);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Cannot read COMSOL application arguments: " + argsFile, ex);
        }
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
        boolean transitionSmokeTest;
        boolean meshConvergence;
        boolean parameterSensitivity;
        boolean parameterAttribution;
        boolean timeConvergence;

        static Cli parse(String[] args) {
            Cli c = new Cli();
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--model": c.model = value(args, ++i, "--model").toLowerCase(); break;
                    case "--material": c.material = value(args, ++i, "--material"); break;
                    case "--c-rate": c.cRate = Double.parseDouble(value(args, ++i, "--c-rate")); break;
                    case "--mode": c.mode = value(args, ++i, "--mode").toLowerCase(); break;
                    case "--config": c.config = value(args, ++i, "--config"); break;
                    case "--full-cell-config": c.fullCellConfig = value(args, ++i, "--full-cell-config"); break;
                    case "--all": c.all = true; break;
                    case "--build-only": c.buildOnly = true; break;
                    case "--solve": break; // solve is the default explicit action
                    case "--export-only": c.exportOnly = true; break;
                    case "--smoke-test": c.smokeTest = true; break;
                    case "--transition-smoke-test":
                        c.smokeTest = true;
                        c.transitionSmokeTest = true;
                        break;
                    case "--mesh-convergence": c.meshConvergence = true; break;
                    case "--parameter-sensitivity": c.parameterSensitivity = true; break;
                    case "--parameter-attribution": c.parameterAttribution = true; break;
                    case "--time-convergence": c.timeConvergence = true; break;
                    case "--help":
                    case "-h": usage(); System.exit(0); break;
                    default: throw new IllegalArgumentException("Unknown argument: " + args[i]);
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
            if (c.mode != null && !Arrays.asList("charge", "discharge", "cycle").contains(c.mode)) {
                throw new IllegalArgumentException("--mode must be charge, discharge, or cycle");
            }
            if (c.model.equals("particle") && (c.parameterSensitivity || c.parameterAttribution || c.timeConvergence)) {
                throw new IllegalArgumentException("sensitivity, attribution, and time convergence require --model full-cell");
            }
            if (c.transitionSmokeTest && !c.model.equals("full-cell")) {
                throw new IllegalArgumentException("--transition-smoke-test requires --model full-cell");
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
            if (c.smokeTest) {
                // Ordinary smoke remains the fixed NFM baseline. A transition
                // smoke may explicitly select NFMZC to qualify its independent
                // charge-cutoff -> discharge handoff.
                if (!c.transitionSmokeTest) c.material = "NFM";
                c.cRate = 1.0;
                c.mode = c.transitionSmokeTest ? "cycle" : "charge";
            }
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
                    + "  --transition-smoke-test (short charge -> discharge handoff)\n"
                    + "  --mesh-convergence | --time-convergence | --parameter-sensitivity | --parameter-attribution\n"
                    + "  --config config/simulation.properties --full-cell-config config/full_cell.properties");
        }
    }
}
