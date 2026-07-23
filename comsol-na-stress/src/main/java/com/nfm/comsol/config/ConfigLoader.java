package com.nfm.comsol.config;

import com.nfm.comsol.util.ValidationUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Collectors;

public final class ConfigLoader {
    private ConfigLoader() {}

    public static SimulationConfig loadSimulation(Path projectRoot, Path file) throws IOException {
        Path resolved = resolve(projectRoot, file);
        Properties p = load(resolved);
        Path output = resolve(projectRoot, Paths.get(required(p, "output.root")));
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
                required(p, "capacity"), required(p, "molar.mass"), required(p, "csmax"),
                number(p, "initial.x"), number(p, "final.x.charge"),
                required(p, "diffusion.charge"), required(p, "diffusion.discharge"),
                required(p, "young.modulus"), doubles(p, "young.modulus.sensitivity.gpa"),
                number(p, "poisson.ratio"),
                doubles(p, "poisson.sensitivity.values"),
                number(p, "chemical.expansion.beta"),
                doubles(p, "chemical.expansion.beta.sensitivity.values"),
                required(p, "parameter.status"),
                required(p, "parameter.source"), number(p, "parameter.uncertainty"),
                resolve(projectRoot, Paths.get(required(p, "parameter.metadata.csv"))),
                resolve(projectRoot, Paths.get(required(p, "ocv.csv"))),
                resolve(projectRoot, Paths.get(required(p, "experimental.curve.csv"))),
                required(p, "exchange.current.density"),
                doubles(p, "exchange.current.density.sensitivity.a_m2"),
                required(p, "diffusion.mode"),
                resolve(projectRoot, Paths.get(required(p, "diffusion.charge.csv"))),
                resolve(projectRoot, Paths.get(required(p, "diffusion.discharge.csv"))),
                required(p, "strain.mode"), resolve(projectRoot, Paths.get(required(p, "strain.csv"))),
                flag(p, "phase.transition.enabled"), number(p, "phase.x.high"),
                number(p, "phase.x.low"), number(p, "phase.extra.strain"),
                number(p, "phase.smoothing.width"), flag(p, "gradient.enabled"),
                number(p, "gradient.exponent"), required(p, "gradient.diffusion.core"),
                required(p, "gradient.diffusion.surface"), number(p, "gradient.beta.core"),
                number(p, "gradient.beta.surface"), file, p);
        ValidationUtils.validateMaterialConfig(cfg);
        if (!Files.isRegularFile(cfg.chargeDiffusionCsv()) || !Files.isRegularFile(cfg.dischargeDiffusionCsv())) {
            throw new IOException("Direction-specific diffusivity CSV not found: "
                    + cfg.chargeDiffusionCsv() + " or " + cfg.dischargeDiffusionCsv());
        }
        if (cfg.strainMode().equals("interpolation") && !Files.isRegularFile(cfg.strainCsv())) {
            throw new IOException("Strain interpolation CSV not found: " + cfg.strainCsv());
        }
        if (!Files.isRegularFile(cfg.ocvCsv())) throw new IOException("Positive-electrode OCV CSV not found: " + cfg.ocvCsv());
        if (!Files.isRegularFile(cfg.parameterMetadataCsv())) {
            throw new IOException("Material parameter metadata CSV not found: " + cfg.parameterMetadataCsv());
        }
        validateParameterMetadata(cfg.parameterMetadataCsv(), cfg.parameterStatus());
        if (!Files.isRegularFile(cfg.experimentalCurveCsv())) {
            throw new IOException("Experimental voltage-capacity CSV not found: " + cfg.experimentalCurveCsv());
        }
        return cfg;
    }

    public static FullCellConfig loadFullCell(Path projectRoot, Path file) throws IOException {
        Path resolved = resolve(projectRoot, file);
        Properties p = load(resolved);
        FullCellConfig cfg = new FullCellConfig(
                required(p, "geometry.classification"),
                required(p, "geometry.anode.length"), required(p, "geometry.separator.length"),
                required(p, "geometry.cathode.length"), required(p, "geometry.width"),
                required(p, "geometry.height"), required(p, "particle.negative.large.radius"),
                required(p, "particle.negative.small.radius"),
                required(p, "electrolyte.initial.concentration"),
                required(p, "electrolyte.conductivity"), required(p, "electrolyte.diffusivity"),
                number(p, "electrolyte.transference.number"),
                number(p, "kinetics.alpha"), required(p, "negative.csmax"),
                number(p, "negative.initial.x"), required(p, "negative.diffusivity"),
                required(p, "negative.young.modulus"), number(p, "negative.poisson.ratio"),
                number(p, "negative.chemical.expansion.beta"),
                doubles(p, "negative.exchange.current.density.sensitivity.a_m2"),
                required(p, "binder.conductivity"), required(p, "binder.young.modulus"),
                number(p, "binder.poisson.ratio"), required(p, "separator.young.modulus"),
                number(p, "separator.poisson.ratio"), number(p, "porosity.anode"),
                number(p, "porosity.cathode"), number(p, "porosity.separator"),
                number(p, "tortuosity.anode"), number(p, "tortuosity.cathode"),
                number(p, "tortuosity.separator"),
                resolve(projectRoot, Paths.get(required(p, "negative.ocv.csv"))),
                resolve(projectRoot, Paths.get(required(p, "negative.kinetics.csv"))),
                resolve(projectRoot, Paths.get(required(p, "negative.diffusivity.csv"))),
                resolve(projectRoot, Paths.get(required(p, "electrolyte.conductivity.csv"))),
                doubles(p, "snapshots.charge.voltages"), doubles(p, "snapshots.discharge.voltages"),
                doubles(p, "snapshots.soc.fractions"),
                number(p, "cutoff.charge.voltage"), number(p, "cutoff.discharge.voltage"),
                required(p, "mesh.max.size"), required(p, "mesh.min.size"),
                number(p, "validation.voltage.rmse.limit"),
                number(p, "validation.voltage.rmse.provisional.limit"),
                number(p, "validation.capacity.error.limit"),
                number(p, "validation.mass.balance.error.limit"),
                number(p, "convergence.average.concentration.limit"),
                number(p, "convergence.concentration.delta.limit"),
                number(p, "convergence.average.stress.limit"),
                number(p, "convergence.stress.p95.limit"), required(p, "parameter.status"),
                required(p, "parameter.source"), number(p, "parameter.uncertainty"),
                resolve(projectRoot, Paths.get(required(p, "parameter.metadata.csv"))), resolved, p);
        ValidationUtils.validateFullCellConfig(cfg);
        for (Path data : Arrays.asList(cfg.negativeOcvCsv(), cfg.negativeKineticsCsv(),
                cfg.negativeDiffusivityCsv(), cfg.electrolyteConductivityCsv(), cfg.parameterMetadataCsv())) {
            if (!Files.isRegularFile(data)) throw new IOException("Full-cell data file not found: " + data);
        }
        validateParameterMetadata(cfg.parameterMetadataCsv(), cfg.parameterStatus());
        return cfg;
    }

    private static void validateParameterMetadata(Path file, String aggregateStatus) throws IOException {
        List<String> lines = Files.readAllLines(file);
        if (lines.size() < 2 || !lines.get(0).trim().equals(
                "parameter,value,unit,status,source,relative_uncertainty")) {
            throw new IOException("Invalid parameter metadata header or empty table: " + file);
        }
        boolean allMeasured = true;
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).trim().isEmpty()) continue;
            String[] columns = lines.get(i).split(",", -1);
            if (columns.length != 6) throw new IOException("Parameter metadata row must have six columns: " + file + ":" + (i+1));
            String status = columns[3].trim().toLowerCase(Locale.ROOT);
            if (!Arrays.asList("provisional", "literature", "measured").contains(status)) {
                throw new IOException("Invalid parameter metadata status: " + file + ":" + (i+1));
            }
            double uncertainty = Double.parseDouble(columns[5].trim());
            if (!Double.isFinite(uncertainty) || uncertainty < 0 || uncertainty > 1) {
                throw new IOException("Invalid parameter metadata uncertainty: " + file + ":" + (i+1));
            }
            allMeasured &= status.equals("measured");
        }
        if (aggregateStatus.equalsIgnoreCase("measured") && !allMeasured) {
            throw new IOException("Aggregate status cannot be measured while metadata contains non-measured rows: " + file);
        }
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
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Missing property: " + key);
        return value.trim();
    }

    private static double number(Properties p, String key) { return Double.parseDouble(required(p, key)); }
    private static boolean flag(Properties p, String key) { return Boolean.parseBoolean(required(p, key)); }
    private static List<Double> doubles(Properties p, String key) {
        return Arrays.stream(required(p, key).split(","))
                .map(String::trim).map(Double::parseDouble).collect(Collectors.toList());
    }
}
