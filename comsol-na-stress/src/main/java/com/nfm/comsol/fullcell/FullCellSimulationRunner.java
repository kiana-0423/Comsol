package com.nfm.comsol.fullcell;

import com.comsol.model.Model;
import com.comsol.model.util.ModelUtil;
import com.nfm.comsol.config.FullCellConfig;
import com.nfm.comsol.config.MaterialConfig;
import com.nfm.comsol.config.SimulationConfig;
import com.nfm.comsol.util.ComsolTagUtils;
import com.nfm.comsol.util.PathUtils;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.io.OutputStream;
import java.util.Properties;

/** Builds, solves, saves and exports one full-cell case. */
public final class FullCellSimulationRunner {
    private final FullCellModelBuilder builder = new FullCellModelBuilder();
    private final FullCellExporter exporter = new FullCellExporter();

    public RunResult run(MaterialConfig material, FullCellConfig cell, SimulationConfig simulation,
                         RunOptions options, SensitivityCase sensitivity) throws Exception {
        PathUtils.ensureOutputTree(simulation.outputRoot());
        String mode = options.mode().equals("cycle") ? "cycle" : options.mode();
        String stem = "FULLCELL_" + PathUtils.caseStem(material.name(), mode, options.cRate())
                + (sensitivity.name().equals("baseline") ? "" : "_" + sensitivity.name());
        Path mph = simulation.outputRoot().resolve("mph").resolve(stem + ".mph");
        snapshotInputs(material, cell, simulation, sensitivity, stem);
        Model model = null;
        try {
            if (options.exportOnly()) {
                if (!Files.isRegularFile(mph)) {
                    throw new java.io.IOException("Existing mph not found for --export-only: " + mph);
                }
                if (!mode.equals("charge")) {
                    throw new IllegalArgumentException(
                            "Full-cell --export-only currently supports --mode charge");
                }
                model = ModelUtil.load("FullCellModel", PathUtils.comsolPath(mph));
                FullCellResultBuilder results = new FullCellResultBuilder();
                results.bindSolution(model, "dset1");
                FullCellExporter.ExportResult exported = exporter.export(model, results, material, cell,
                        simulation, "dset1", "charge", stem + "_charge");
                int elements = model.component(ComsolTagUtils.FULL_COMPONENT)
                        .mesh(ComsolTagUtils.FULL_MESH).getNumElem();
                return new RunResult(stem, mph, exported.metrics(), exported.maximumStress(),
                        exported.averageStress(), exported.stressP95(), exported.averageX(),
                        exported.concentrationDelta(), exported.massBalanceError(), elements,
                        exported.quantitativeReady());
            }
            FullCellModelBuilder.BuiltFullCell built =
                    builder.build(material, cell, simulation, options.cRate(), options.smokeTest(), sensitivity);
            model = built.model();
            if (options.buildOnly()) {
                model.save(PathUtils.comsolPath(mph));
                return new RunResult(stem, mph, null, Double.NaN, Double.NaN, Double.NaN,
                        Double.NaN, Double.NaN, Double.NaN, -1, false);
            }

            model.param().set("runDirection", "1");
            String chargeSolution = built.studies().prepareAndRun(model,
                    ComsolTagUtils.FULL_STUDY_CHARGE, "sol1",
                    simulation, false, options.smokeTest());
            System.out.println("Charge solution sequence: " + chargeSolution);
            String chargeDataset = findSolutionDataset(model, chargeSolution, "dset1");
            built.results().bindSolution(model, chargeDataset);
            // Preserve the converged solution before postprocessing so an export failure
            // never discards a successful, potentially expensive solve.
            model.save(PathUtils.comsolPath(mph));
            FullCellExporter.ExportResult exported = exporter.export(model, built.results(), material, cell,
                    simulation, chargeDataset, "charge", stem + "_charge");
            double peakStress = exported.maximumStress();
            double maximumAverageStress = exported.averageStress();
            double stressP95 = exported.stressP95();
            double finalAverageX = exported.averageX();
            double maximumConcentrationDelta = exported.concentrationDelta();
            double worstMassError = exported.massBalanceError();
            boolean quantitativeReady = exported.quantitativeReady();

            if (options.mode().equals("discharge") || options.mode().equals("cycle")) {
                model.param().set("runDirection", "-1");
                // A charge cutoff can leave the negative electrode too close to
                // depletion to sustain even a microsecond of further charge.
                // Start discharge at 0.1 of the requested rate (a reversal
                // already qualified by the 0.1C cycles), then reach the full
                // requested discharge current smoothly over 10 seconds.
                model.component(ComsolTagUtils.FULL_COMPONENT)
                        .physics(ComsolTagUtils.FULL_BATTERY).feature("current_pos")
                        .set("Ias", options.cRate() >= 0.5
                                ? "-j_app*(0.1+0.9*min(t/10[s],1))"
                                : "runDirection*j_app");
                String dischargeSolution = built.studies().prepareAndRun(model,
                        ComsolTagUtils.FULL_STUDY_DISCHARGE, "sol2",
                        simulation, true, options.smokeTest());
                System.out.println("Discharge solution sequence: " + dischargeSolution);
                String dischargeDataset = findSolutionDataset(model, dischargeSolution, "dset2");
                built.results().bindSolution(model, dischargeDataset);
                // Preserve both converged cycle legs before postprocessing, just
                // as for charge, so export failures cannot discard the solve.
                model.save(PathUtils.comsolPath(mph));
                exported = exporter.export(model, built.results(), material, cell,
                        simulation, dischargeDataset, "discharge", stem + "_discharge");
                peakStress = Math.max(peakStress, exported.maximumStress());
                maximumAverageStress = Math.max(maximumAverageStress, exported.averageStress());
                stressP95 = Math.max(stressP95, exported.stressP95());
                finalAverageX = exported.averageX();
                maximumConcentrationDelta = Math.max(maximumConcentrationDelta, exported.concentrationDelta());
                worstMassError = Math.max(worstMassError, exported.massBalanceError());
                quantitativeReady = quantitativeReady && exported.quantitativeReady();
            }
            model.save(PathUtils.comsolPath(mph));
            int elements = model.component(ComsolTagUtils.FULL_COMPONENT).mesh(ComsolTagUtils.FULL_MESH).getNumElem();
            return new RunResult(stem, mph, exported.metrics(), peakStress, maximumAverageStress,
                    stressP95, finalAverageX, maximumConcentrationDelta,
                    worstMassError, elements, quantitativeReady);
        } finally {
            if (model != null) ModelUtil.remove(model.tag());
        }
    }

    private String findSolutionDataset(Model model, String solutionTag, String fallback) {
        for (String datasetTag : model.result().dataset().tags()) {
            if (!model.result().dataset(datasetTag).hasProperty("solution")) continue;
            String datasetSolution = model.result().dataset(datasetTag).getString("solution");
            System.out.println("Solution dataset: " + datasetTag + " -> " + datasetSolution);
            if (solutionTag.equals(datasetSolution)) return datasetTag;
        }
        System.out.println("WARNING: no dataset explicitly references " + solutionTag
                + "; trying fallback " + fallback);
        return fallback;
    }

    private void snapshotInputs(MaterialConfig material, FullCellConfig cell, SimulationConfig simulation,
                                SensitivityCase sensitivity, String stem) throws Exception {
        Path dir = simulation.outputRoot().resolve("logs").resolve(stem + "_" + PathUtils.timestamp() + "_config");
        Files.createDirectories(dir);
        for (Path source : new Path[]{material.sourceFile(), cell.sourceFile(), simulation.sourceFile(),
                material.parameterMetadataCsv(), cell.parameterMetadataCsv(),
                material.ocvCsv(), material.experimentalCurveCsv(), material.chargeDiffusionCsv(),
                material.dischargeDiffusionCsv(), material.strainCsv(), cell.negativeOcvCsv(),
                cell.negativeKineticsCsv(), cell.negativeDiffusivityCsv(), cell.electrolyteConductivityCsv()}) {
            Files.copy(source, dir.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
        Properties manifest = new Properties();
        manifest.setProperty("material", material.name());
        manifest.setProperty("material.formula", material.formula());
        manifest.setProperty("material.status", material.parameterStatus());
        manifest.setProperty("material.source", material.parameterSource());
        manifest.setProperty("cell.status", cell.parameterStatus());
        manifest.setProperty("cell.source", cell.parameterSource());
        manifest.setProperty("sensitivity.case", sensitivity.name());
        manifest.setProperty("sensitivity.diffusion.charge.scale", Double.toString(sensitivity.chargeDiffusionScale()));
        manifest.setProperty("sensitivity.diffusion.discharge.scale", Double.toString(sensitivity.dischargeDiffusionScale()));
        manifest.setProperty("sensitivity.strain.scale", Double.toString(sensitivity.strainScale()));
        manifest.setProperty("sensitivity.modulus.scale", Double.toString(sensitivity.modulusScale()));
        manifest.setProperty("sensitivity.poisson.scale", Double.toString(sensitivity.poissonScale()));
        manifest.setProperty("sensitivity.radius.scale", Double.toString(sensitivity.radiusScale()));
        manifest.setProperty("sensitivity.kinetics.positive.scale",
                Double.toString(sensitivity.positiveKineticsScale()));
        manifest.setProperty("sensitivity.kinetics.negative.scale",
                Double.toString(sensitivity.negativeKineticsScale()));
        try (OutputStream out = Files.newOutputStream(dir.resolve("manifest.properties"))) {
            manifest.store(out, "Full-cell run input manifest");
        }
    }

    public static final class RunOptions {
        private final double cRate;
        private final String mode;
        private final boolean buildOnly, exportOnly, smokeTest;

        public RunOptions(double cRate, String mode, boolean buildOnly, boolean smokeTest) {
            this(cRate, mode, buildOnly, false, smokeTest);
        }

        public RunOptions(double cRate, String mode, boolean buildOnly,
                          boolean exportOnly, boolean smokeTest) {
            this.cRate = cRate;
            this.mode = mode;
            this.buildOnly = buildOnly;
            this.exportOnly = exportOnly;
            this.smokeTest = smokeTest;
        }

        public double cRate() { return cRate; }
        public String mode() { return mode; }
        public boolean buildOnly() { return buildOnly; }
        public boolean exportOnly() { return exportOnly; }
        public boolean smokeTest() { return smokeTest; }
    }

    public static final class RunResult {
        private final String stem;
        private final Path mph, metrics;
        private final double maximumStress, maximumAverageStress, stressP95, finalAverageX;
        private final double maximumConcentrationDelta, massBalanceError;
        private final int elementCount;
        private final boolean quantitativeReady;

        public RunResult(String stem, Path mph, Path metrics, double maximumStress,
                         double maximumAverageStress, double stressP95, double finalAverageX,
                         double maximumConcentrationDelta, double massBalanceError,
                         int elementCount, boolean quantitativeReady) {
            this.stem = stem;
            this.mph = mph;
            this.metrics = metrics;
            this.maximumStress = maximumStress;
            this.maximumAverageStress = maximumAverageStress;
            this.stressP95 = stressP95;
            this.finalAverageX = finalAverageX;
            this.maximumConcentrationDelta = maximumConcentrationDelta;
            this.massBalanceError = massBalanceError;
            this.elementCount = elementCount;
            this.quantitativeReady = quantitativeReady;
        }

        public String stem() { return stem; }
        public Path mph() { return mph; }
        public Path metrics() { return metrics; }
        public double maximumStress() { return maximumStress; }
        public double maximumAverageStress() { return maximumAverageStress; }
        public double stressP95() { return stressP95; }
        public double finalAverageX() { return finalAverageX; }
        public double maximumConcentrationDelta() { return maximumConcentrationDelta; }
        public double massBalanceError() { return massBalanceError; }
        public int elementCount() { return elementCount; }
        public boolean quantitativeReady() { return quantitativeReady; }
    }
}
