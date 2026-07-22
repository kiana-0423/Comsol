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
        snapshotInputs(material, cell, simulation, sensitivity, stem);
        Model model = null;
        try {
            var built = builder.build(material, cell, simulation, options.cRate(), options.smokeTest(), sensitivity);
            model = built.model();
            Path mph = simulation.outputRoot().resolve("mph").resolve(stem + ".mph");
            if (options.buildOnly()) {
                model.save(PathUtils.comsolPath(mph));
                return new RunResult(stem, mph, null, Double.NaN, Double.NaN, Double.NaN,
                        Double.NaN, Double.NaN, Double.NaN, -1, false);
            }

            model.study(ComsolTagUtils.FULL_STUDY_INIT).run();
            model.param().set("runDirection", "1");
            built.studies().prepareAndRun(model, ComsolTagUtils.FULL_STUDY_CHARGE, "sol2", simulation, false);
            built.results().bindSolution(model, "dset2");
            FullCellExporter.ExportResult exported = exporter.export(model, built.results(), material, cell,
                    simulation, "dset2", "charge", stem + "_charge");
            double peakStress = exported.maximumStress();
            double maximumAverageStress = exported.averageStress();
            double stressP95 = exported.stressP95();
            double finalAverageX = exported.averageX();
            double maximumConcentrationDelta = exported.concentrationDelta();
            double worstMassError = exported.massBalanceError();
            boolean quantitativeReady = exported.quantitativeReady();

            if (options.mode().equals("discharge") || options.mode().equals("cycle")) {
                model.param().set("runDirection", "-1");
                built.studies().prepareAndRun(model, ComsolTagUtils.FULL_STUDY_DISCHARGE, "sol3", simulation, true);
                built.results().bindSolution(model, "dset3");
                exported = exporter.export(model, built.results(), material, cell,
                        simulation, "dset3", "discharge", stem + "_discharge");
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
        manifest.setProperty("sensitivity.kinetics.scale", Double.toString(sensitivity.kineticsScale()));
        try (OutputStream out = Files.newOutputStream(dir.resolve("manifest.properties"))) {
            manifest.store(out, "Full-cell run input manifest");
        }
    }

    public record RunOptions(double cRate, String mode, boolean buildOnly, boolean smokeTest) {}
    public record RunResult(String stem, Path mph, Path metrics, double maximumStress,
                            double maximumAverageStress, double stressP95, double finalAverageX,
                            double maximumConcentrationDelta, double massBalanceError,
                            int elementCount, boolean quantitativeReady) {}
    public record SensitivityCase(String name, double chargeDiffusionScale,
                                  double dischargeDiffusionScale, double strainScale,
                                  double modulusScale, double poissonScale,
                                  double radiusScale, double kineticsScale) {
        public static SensitivityCase baseline() { return new SensitivityCase("baseline", 1, 1, 1, 1, 1, 1, 1); }
    }
}
