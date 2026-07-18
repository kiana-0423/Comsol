package com.nfm.comsol;

import com.comsol.model.util.ModelUtil;
import com.nfm.comsol.config.ConfigLoader;
import com.nfm.comsol.config.MaterialConfig;
import com.nfm.comsol.config.SimulationConfig;
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
            String mode = cli.mode == null ? simulation.defaultMode() : cli.mode;

            if (!ModelUtil.getComsolVersion().startsWith("6.4")) {
                throw new IllegalStateException("COMSOL 6.4 required, found " + ModelUtil.getComsolVersion());
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

    /** comsolbatch owns its command line, so Windows scripts pass application arguments via this variable. */
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
        double cRate = 1.0;
        String mode;
        String config;
        boolean all;
        boolean buildOnly;
        boolean exportOnly;
        boolean smokeTest;
        boolean meshConvergence;

        static Cli parse(String[] args) {
            Cli c = new Cli();
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--material" -> c.material = value(args, ++i, "--material");
                    case "--c-rate" -> c.cRate = Double.parseDouble(value(args, ++i, "--c-rate"));
                    case "--mode" -> c.mode = value(args, ++i, "--mode").toLowerCase();
                    case "--config" -> c.config = value(args, ++i, "--config");
                    case "--all" -> c.all = true;
                    case "--build-only" -> c.buildOnly = true;
                    case "--solve" -> { /* solve is the default explicit action */ }
                    case "--export-only" -> c.exportOnly = true;
                    case "--smoke-test" -> c.smokeTest = true;
                    case "--mesh-convergence" -> c.meshConvergence = true;
                    case "--help", "-h" -> { usage(); System.exit(0); }
                    default -> throw new IllegalArgumentException("Unknown argument: " + args[i]);
                }
            }
            if (c.buildOnly && c.exportOnly) throw new IllegalArgumentException("--build-only and --export-only are mutually exclusive");
            if (c.meshConvergence && (c.all || c.buildOnly || c.exportOnly || c.smokeTest)) {
                throw new IllegalArgumentException("--mesh-convergence cannot be combined with --all/build-only/export-only/smoke-test");
            }
            if (c.smokeTest) { c.material = "NFM"; c.cRate = 1.0; c.mode = "charge"; }
            return c;
        }

        private static String value(String[] args, int index, String option) {
            if (index >= args.length) throw new IllegalArgumentException("Missing value after " + option);
            return args[index];
        }

        private static void usage() {
            System.out.println("COMSOL Na particle model (Windows COMSOL 6.4)\n"
                    + "  --material NFM|NFMZC --c-rate 0.1|0.5|1|2 --mode charge|discharge\n"
                    + "  --all | --build-only | --solve | --export-only | --smoke-test | --mesh-convergence\n"
                    + "  --config config/simulation.properties");
        }
    }
}
