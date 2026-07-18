package com.nfm.comsol.config;

import com.nfm.comsol.util.ValidationUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public final class ConfigLoader {
    private ConfigLoader() {}

    public static SimulationConfig loadSimulation(Path projectRoot, Path file) throws IOException {
        Path resolved = resolve(projectRoot, file);
        Properties p = load(resolved);
        Path output = resolve(projectRoot, Path.of(required(p, "output.root")));
        SimulationConfig cfg = new SimulationConfig(
                required(p, "comsol.version"), doubles(p, "c.rates"), required(p, "mode"),
                required(p, "discharge.initialization"), required(p, "mesh.level"),
                required(p, "mesh.max.size"), required(p, "mesh.min.size"),
                number(p, "mesh.growth.rate"), number(p, "mesh.convergence.tolerance"),
                number(p, "solver.relative.tolerance"), doubles(p, "solver.output.points"),
                number(p, "solver.max.step.fraction"), required(p, "smoke.duration"),
                required(p, "smoke.mesh.size"), output,
                number(p, "figures.concentration.min"), number(p, "figures.concentration.max"),
                required(p, "figures.stress.min"), required(p, "figures.stress.max"),
                doubles(p, "export.progress.points"), required(p, "validation.initial.stress.tolerance"),
                projectRoot, resolved, p);
        ValidationUtils.validateSimulationConfig(cfg);
        return cfg;
    }

    public static MaterialConfig loadMaterial(Path projectRoot, String material) throws IOException {
        String id = material.toLowerCase(Locale.ROOT);
        if (!id.equals("nfm") && !id.equals("nfmzc")) {
            throw new IllegalArgumentException("Unsupported material: " + material + "; expected NFM or NFMZC");
        }
        Path file = projectRoot.resolve("config").resolve(id + ".properties").normalize();
        Properties p = load(file);
        MaterialConfig cfg = new MaterialConfig(
                required(p, "material.name"), required(p, "material.formula"),
                required(p, "particle.radius"), required(p, "particle.density"),
                required(p, "capacity"), required(p, "csmax"), number(p, "initial.x"),
                number(p, "final.x.charge"), required(p, "diffusion.coefficient"),
                required(p, "young.modulus"), number(p, "poisson.ratio"),
                number(p, "chemical.expansion.beta"), required(p, "parameter.status"),
                required(p, "diffusion.mode"), resolve(projectRoot, Path.of(required(p, "diffusion.csv"))),
                required(p, "strain.mode"), resolve(projectRoot, Path.of(required(p, "strain.csv"))),
                flag(p, "phase.transition.enabled"), number(p, "phase.x.start"),
                number(p, "phase.x.end"), number(p, "phase.extra.strain"),
                number(p, "phase.smoothing.width"), flag(p, "gradient.enabled"),
                number(p, "gradient.exponent"), required(p, "gradient.diffusion.core"),
                required(p, "gradient.diffusion.surface"), number(p, "gradient.beta.core"),
                number(p, "gradient.beta.surface"), file, p);
        ValidationUtils.validateMaterialConfig(cfg);
        if (cfg.diffusionMode().equals("interpolation") && !Files.isRegularFile(cfg.diffusionCsv())) {
            throw new IOException("Diffusivity interpolation CSV not found: " + cfg.diffusionCsv());
        }
        if (cfg.strainMode().equals("interpolation") && !Files.isRegularFile(cfg.strainCsv())) {
            throw new IOException("Strain interpolation CSV not found: " + cfg.strainCsv());
        }
        return cfg;
    }

    private static Properties load(Path file) throws IOException {
        if (!Files.isRegularFile(file)) throw new IOException("Configuration file not found: " + file);
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) { p.load(in); }
        return p;
    }

    private static Path resolve(Path root, Path path) {
        return (path.isAbsolute() ? path : root.resolve(path)).normalize().toAbsolutePath();
    }

    private static String required(Properties p, String key) {
        String value = p.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing property: " + key);
        return value.trim();
    }

    private static double number(Properties p, String key) { return Double.parseDouble(required(p, key)); }
    private static boolean flag(Properties p, String key) { return Boolean.parseBoolean(required(p, key)); }
    private static List<Double> doubles(Properties p, String key) {
        return Arrays.stream(required(p, key).split(",")).map(String::trim).map(Double::parseDouble).toList();
    }
}
