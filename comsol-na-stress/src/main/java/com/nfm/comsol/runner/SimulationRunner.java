package com.nfm.comsol.runner;

import com.comsol.model.Model;
import com.comsol.model.util.ModelUtil;
import com.nfm.comsol.config.MaterialConfig;
import com.nfm.comsol.config.SimulationConfig;
import com.nfm.comsol.export.CsvExporter;
import com.nfm.comsol.export.FigureExporter;
import com.nfm.comsol.export.MetricsExporter;
import com.nfm.comsol.model.ParticleModelBuilder;
import com.nfm.comsol.model.ResultBuilder;
import com.nfm.comsol.util.ComsolTagUtils;
import com.nfm.comsol.util.PathUtils;
import com.nfm.comsol.util.ValidationUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.SimpleFormatter;

public final class SimulationRunner {
    private final ParticleModelBuilder builder = new ParticleModelBuilder();
    private final CsvExporter csvExporter = new CsvExporter();
    private final MetricsExporter metricsExporter = new MetricsExporter();
    private final FigureExporter figureExporter = new FigureExporter();

    public RunResult run(MaterialConfig material, SimulationConfig simulation, RunOptions options) throws Exception {
        ValidationUtils.validateRun(options.mode(), options.cRate());
        PathUtils.ensureOutputTree(simulation.outputRoot());
        ValidationUtils.validateOutputRoot(simulation);
        String stem = PathUtils.caseStem(material.name(), options.mode(), options.cRate());
        Logger logger = createLogger(simulation.outputRoot(), stem);
        Instant start = Instant.now();
        Model model = null;
        try {
            logHeader(logger, material, simulation, options, start);
            snapshotConfiguration(material, simulation, stem);
            if (options.exportOnly()) {
                Path mph = simulation.outputRoot().resolve("mph").resolve(stem + ".mph");
                if (!Files.isRegularFile(mph)) throw new IOException("Existing mph not found for --export-only: " + mph);
                model = ModelUtil.load("ParticleModel", PathUtils.comsolPath(mph));
                return export(model, null, simulation, stem, logger, mph);
            }

            ParticleModelBuilder.BuiltModel built =
                    builder.build(material, simulation, options.cRate(), options.mode(), options.smokeTest());
            model = built.model();
            Path mph = simulation.outputRoot().resolve("mph").resolve(stem + ".mph");
            if (options.buildOnly()) {
                model.save(PathUtils.comsolPath(mph));
                logger.info("solver status=NOT_RUN (build-only); output mph=" + mph);
                return new RunResult(stem, mph, null, null, null, Double.NaN, Double.NaN, -1);
            }

            solve(model, built, simulation, options, logger);
            built.results().bindSolution(model, options.mode());
            model.save(PathUtils.comsolPath(mph));
            RunResult result = export(model, built, simulation, stem, logger, mph);
            validateDirection(options.mode(), result.averageXNa(), material);
            logger.info("solver status=SUCCESS; end time=" + Instant.now());
            return result;
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "solver status=FAILED; end time=" + Instant.now(), ex);
            throw ex;
        } finally {
            if (model != null) ModelUtil.remove(model.tag());
            for (Handler handler : logger.getHandlers()) handler.close();
        }
    }

    private void solve(Model model, ParticleModelBuilder.BuiltModel built, SimulationConfig simulation,
                       RunOptions options, Logger logger) {
        if (options.mode().equals("discharge")) {
            if (!simulation.dischargeInitialization().equals("continue_charge")) {
                throw new UnsupportedOperationException("Only discharge.initialization=continue_charge is implemented; no pseudo-discharge is generated.");
            }
            model.param().set("runSign", "chargeSign");
            logger.info("Solving prerequisite charge study for discharge continuation");
            built.studies().prepareAndRun(model, ComsolTagUtils.STUDY_CHARGE, "sol1", "tCharge", simulation);
            model.param().set("runSign", "dischargeSign");
            built.studies().configureDischargeContinuation(model);
            built.studies().prepareAndRun(model, ComsolTagUtils.STUDY_DISCHARGE, "sol2", "tDischarge", simulation);
        } else {
            model.param().set("runSign", "chargeSign");
            built.studies().prepareAndRun(model, ComsolTagUtils.STUDY_CHARGE, "sol1", "tCharge", simulation);
        }
    }

    private RunResult export(Model model, ParticleModelBuilder.BuiltModel built, SimulationConfig simulation,
                             String stem, Logger logger, Path mph) throws IOException {
        ResultBuilder resultBuilder = built != null ? built.results() : new ResultBuilder();
        Path radial = csvExporter.exportRadialProfiles(model, simulation.outputRoot().resolve("csv"), stem);
        MetricsExporter.ExportedMetrics metrics =
                metricsExporter.export(model, resultBuilder, simulation.outputRoot().resolve("csv"), stem);
        figureExporter.export(model, simulation, simulation.outputRoot().resolve("figures"), stem);
        double avgX = metrics.value("average_xNa");
        double maxStress = metrics.value("maximum_von_mises_Pa");
        double delta = metrics.value("surface_center_delta_xNa");
        double inventoryError = metrics.value("spherical_inventory_relative_error");
        double initialStressTolerance = parseLeadingDouble(simulation.initialStressTolerance());
        if (stem.contains("_charge_") && Math.abs(metrics.initialMaximumStress()) > initialStressTolerance) {
            throw new IllegalStateException("Initial stress validation failed: " + metrics.initialMaximumStress()
                    + " Pa exceeds " + simulation.initialStressTolerance());
        }
        if (inventoryError > 0.01) {
            throw new IllegalStateException("Spherical-particle inventory validation failed: "
                    + inventoryError + " exceeds 1%");
        }
        int elements = model.component(ComsolTagUtils.COMPONENT).mesh(ComsolTagUtils.MESH).getNumElem();
        logger.info("output mph=" + mph + "; output csv=" + metrics.metricsFile() + "; elements=" + elements);
        return new RunResult(stem, mph, metrics.metricsFile(), radial, metrics.timeSeriesFile(), avgX, maxStress, elements, delta);
    }

    private void validateDirection(String mode, double finalAverageX, MaterialConfig material) {
        if (mode.equals("charge") && !(finalAverageX < material.initialX())) {
            throw new IllegalStateException("Charge validation failed: average xNa did not decrease");
        }
        if (mode.equals("discharge") && !(finalAverageX > material.finalChargeX())) {
            throw new IllegalStateException("Discharge validation failed: average xNa did not increase from charged state");
        }
    }

    private double parseLeadingDouble(String expression) {
        int bracket = expression.indexOf('[');
        return Double.parseDouble((bracket < 0 ? expression : expression.substring(0, bracket)).trim());
    }

    private Logger createLogger(Path outputRoot, String stem) throws IOException {
        Logger logger = Logger.getLogger("com.nfm.comsol." + stem + "." + System.nanoTime());
        logger.setUseParentHandlers(true);
        FileHandler handler = new FileHandler(outputRoot.resolve("logs").resolve("run_" + PathUtils.timestamp() + ".log").toString());
        handler.setFormatter(new SimpleFormatter());
        logger.addHandler(handler);
        return logger;
    }

    private void logHeader(Logger log, MaterialConfig m, SimulationConfig s, RunOptions o, Instant start) {
        log.info("start time=" + start + "; COMSOL version=" + ModelUtil.getComsolVersion()
                + "; expected COMSOL=" + s.comsolVersion() + "; Java=" + System.getProperty("java.version"));
        log.info("material=" + m.name() + "; formula=" + m.formula() + "; C-rate=" + o.cRate()
                + "; mode=" + o.mode() + "; mesh=" + s.meshLevel());
        log.info("material file=" + m.sourceFile() + "; simulation file=" + s.sourceFile()
                + "; output=" + s.outputRoot());
        log.info("key parameters: Rp=" + m.radius() + ", Ds_charge=" + m.chargeDiffusivity()
                + ", Ds_discharge=" + m.dischargeDiffusivity() + ", E="
                + m.youngModulus() + ", nu=" + m.poissonRatio() + ", beta=" + m.beta()
                + ", status=" + m.parameterStatus() + ", source=" + m.parameterSource());
    }

    private void snapshotConfiguration(MaterialConfig m, SimulationConfig s, String stem) throws IOException {
        Path dir = s.outputRoot().resolve("logs").resolve(stem + "_" + PathUtils.timestamp() + "_config");
        Files.createDirectories(dir);
        Files.copy(m.sourceFile(), dir.resolve(m.sourceFile().getFileName()), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(s.sourceFile(), dir.resolve(s.sourceFile().getFileName()), StandardCopyOption.REPLACE_EXISTING);
        if (m.diffusionMode().equals("interpolation")) {
            Files.copy(m.chargeDiffusionCsv(), dir.resolve(m.chargeDiffusionCsv().getFileName()), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(m.dischargeDiffusionCsv(), dir.resolve(m.dischargeDiffusionCsv().getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
        if (m.strainMode().equals("interpolation")) Files.copy(m.strainCsv(), dir.resolve(m.strainCsv().getFileName()), StandardCopyOption.REPLACE_EXISTING);
    }

    public static final class RunOptions {
        private final double cRate;
        private final String mode;
        private final boolean buildOnly, exportOnly, smokeTest;

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
        private final Path mph, metrics, radialProfiles, timeSeries;
        private final double averageXNa, maximumStress, surfaceCenterDelta;
        private final int elementCount;

        public RunResult(String stem, Path mph, Path metrics, Path radialProfiles, Path timeSeries,
                         double averageXNa, double maximumStress, int elementCount,
                         double surfaceCenterDelta) {
            this.stem = stem;
            this.mph = mph;
            this.metrics = metrics;
            this.radialProfiles = radialProfiles;
            this.timeSeries = timeSeries;
            this.averageXNa = averageXNa;
            this.maximumStress = maximumStress;
            this.elementCount = elementCount;
            this.surfaceCenterDelta = surfaceCenterDelta;
        }

        public RunResult(String stem, Path mph, Path metrics, Path radialProfiles, Path timeSeries,
                         double averageXNa, double maximumStress, int elementCount) {
            this(stem, mph, metrics, radialProfiles, timeSeries, averageXNa, maximumStress, elementCount, Double.NaN);
        }

        public String stem() { return stem; }
        public Path mph() { return mph; }
        public Path metrics() { return metrics; }
        public Path radialProfiles() { return radialProfiles; }
        public Path timeSeries() { return timeSeries; }
        public double averageXNa() { return averageXNa; }
        public double maximumStress() { return maximumStress; }
        public int elementCount() { return elementCount; }
        public double surfaceCenterDelta() { return surfaceCenterDelta; }
    }
}
