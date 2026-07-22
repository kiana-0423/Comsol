package com.nfm.comsol.fullcell;

import com.comsol.model.Model;
import com.nfm.comsol.config.FullCellConfig;
import com.nfm.comsol.config.MaterialConfig;
import com.nfm.comsol.config.SimulationConfig;
import com.nfm.comsol.util.ComsolTagUtils;
import com.nfm.comsol.util.PathUtils;
import com.nfm.comsol.util.ValidationUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Exports voltage-addressed figures, time series, radial profiles and acceptance metadata. */
public final class FullCellExporter {
    public ExportResult export(Model model, FullCellResultBuilder results, MaterialConfig material,
                               FullCellConfig cell, SimulationConfig simulation, String dataset,
                               String mode, String stem) throws IOException {
        Path csvDir = simulation.outputRoot().resolve("csv");
        Path figureDir = simulation.outputRoot().resolve("figures");
        Map<String, String> metrics = results.metrics();
        String id = "r" + Integer.toHexString(stem.hashCode());
        List<String> names = new ArrayList<>(metrics.keySet());
        String[] expressions = names.stream().map(metrics::get).toArray(String[]::new);

        String tableTag = "tbl_" + id;
        String evalTag = "eval_" + id;
        model.result().table().create(tableTag, "Table");
        model.result().numerical().create(evalTag, "EvalGlobal");
        var eval = model.result().numerical(evalTag);
        eval.set("data", dataset);
        eval.set("expr", expressions);
        eval.set("descr", names.toArray(String[]::new));
        eval.set("looplevelinput", "all");
        eval.set("table", tableTag);
        eval.setResult();

        Path timeSeries = csvDir.resolve(stem + "_time_series.csv");
        String timeExportTag = "ts_" + id;
        model.result().export().create(timeExportTag, "Table");
        model.result().export(timeExportTag).set("table", tableTag);
        model.result().export(timeExportTag).set("filename", PathUtils.comsolPath(timeSeries));
        model.result().export(timeExportTag).run();

        List<Double> finalValues = new ArrayList<>();
        for (int i = 0; i < expressions.length; i++) {
            double[] values = series(model, "scalar_" + id + "_" + i, dataset, expressions[i]);
            double value = values.length == 0 ? Double.NaN : values[values.length - 1];
            ValidationUtils.requireFinite(names.get(i), value);
            finalValues.add(value);
        }
        Path metricsFile = csvDir.resolve(stem + "_metrics.csv");
        writeMetrics(metricsFile, names, finalValues);

        Path radial = csvDir.resolve(stem + "_positive_radial_profiles.csv");
        String radialTag = "radial_" + id;
        model.result().export().create(radialTag, "Data");
        var radialExport = model.result().export(radialTag);
        radialExport.set("data", ComsolTagUtils.FULL_DATASET_POSITIVE_CUTLINE);
        radialExport.set("expr", new String[]{"sqrt((x-x_pos_center)^2+(y-y_pos_center)^2+(z-z_pos_center)^2)/Rp_pos",
                "xPos", "cPos", "solid_full.mises", "epsilonChemPos"});
        radialExport.set("unit", new String[]{"1", "1", "mol/m^3", "MPa", "1"});
        radialExport.set("looplevelinput", "all");
        radialExport.set("filename", PathUtils.comsolPath(radial));
        radialExport.set("fullprec", true);
        radialExport.run();

        double[] voltages = series(model, "voltage_" + id, dataset, "cellVoltage/1[V]");
        if (voltages.length == 0) throw new IOException("No voltage solution levels available for " + stem);
        double[] capacities = series(model, "capacity_" + id, dataset,
                "abs(I_app*t)/mp_pos/1[Ah/kg]");
        double[] rates = series(model, "crate_" + id, dataset, "C_rate");
        double[] averageX = series(model, "average_x_" + id, dataset, "ave_pos(xPos)");
        double[] stressSeries = series(model, "stress_" + id, dataset, "max_pos(solid_full.mises)");
        double[] averageStressSeries = series(model, "average_stress_" + id, dataset,
                "ave_pos(solid_full.mises)");
        double[] concentrationDeltaSeries = series(model, "concentration_delta_" + id, dataset,
                "abs(ave_pos_surface(xPos)-at3(x_pos_center,y_pos_center,z_pos_center,xPos))");
        int peakStressLevel = maximumIndex(stressSeries)+1;
        Path stressSamples = csvDir.resolve(stem + "_stress_samples_at_peak.csv");
        double stressP95 = exportAndCalculateStressP95(model, dataset, id, peakStressLevel,
                stressSamples);
        List<Double> targets = mode.equals("charge") ? cell.chargeSnapshotVoltages() : cell.dischargeSnapshotVoltages();
        Path snapshotIndex = csvDir.resolve(stem + "_snapshot_index.csv");
        exportVoltageFigures(model, figureDir, stem, id, voltages, targets, snapshotIndex);
        Path socSnapshotIndex = csvDir.resolve(stem + "_soc_snapshot_index.csv");
        exportSocFigures(model, figureDir, stem, id, averageX, material,
                cell.snapshotSocFractions(), socSnapshotIndex);

        double massError = finalValues.get(names.indexOf("mass_balance_relative_error"));
        boolean massPass = massError <= cell.massBalanceErrorLimit();
        double targetVoltage = mode.equals("charge") ? cell.chargeCutoffVoltage() : cell.dischargeCutoffVoltage();
        double finalVoltage = voltages.length == 0 ? Double.NaN : voltages[voltages.length-1];
        double cutoffError = Math.abs(finalVoltage-targetVoltage);
        boolean cutoffPass = cutoffError <= cell.voltageRmseLimit();
        boolean monotonicPass = monotonic(averageX, mode.equals("charge"));
        double initialStress = stressSeries.length == 0 ? Double.NaN : stressSeries[0];
        double peakStress = java.util.Arrays.stream(stressSeries).max().orElse(Double.NaN);
        double finalAverageX = finalValues.get(names.indexOf("average_positive_xNa"));
        double maximumConcentrationDelta = java.util.Arrays.stream(concentrationDeltaSeries).max().orElse(Double.NaN);
        double maximumAverageStress = java.util.Arrays.stream(averageStressSeries).max().orElse(Double.NaN);
        double initialStressLimit = parseLeadingDouble(simulation.initialStressTolerance());
        boolean initialStressApplicable = mode.equals("charge");
        boolean initialStressPass = initialStressApplicable && Math.abs(initialStress) <= initialStressLimit;
        double negativeCapacityRatio = finalValues.get(names.indexOf("negative_capacity_ratio"));
        boolean negativeCapacityPass = negativeCapacityRatio >= 1.0;
        boolean quantitativeReady = material.parameterStatus().equalsIgnoreCase("measured")
                && cell.parameterStatus().equalsIgnoreCase("measured")
                && material.strainMode().equals("interpolation");
        double activeVoltageRmseLimit = quantitativeReady
                ? cell.voltageRmseLimit() : cell.provisionalVoltageRmseLimit();
        double cRate = rates.length == 0 ? Double.NaN : rates[0];
        boolean isCalibrationRun = mode.equals("charge") && Math.abs(cRate - 0.1) < 1e-9;
        CurveValidation curveValidation = isCalibrationRun
                ? validateExperimentalCurve(material.experimentalCurveCsv(), capacities, voltages)
                : CurveValidation.notApplicable();
        Path acceptance = csvDir.resolve(stem + "_acceptance.csv");
        try (BufferedWriter out = Files.newBufferedWriter(acceptance, StandardCharsets.UTF_8)) {
            out.write("check,value,limit,pass\n");
            out.write("mass_balance_relative_error," + massError + "," + cell.massBalanceErrorLimit() + "," + massPass + "\n");
            out.write("cutoff_voltage_error_V," + cutoffError + "," + cell.voltageRmseLimit() + "," + cutoffPass + "\n");
            out.write("positive_xNa_monotonic," + monotonicPass + ",1," + monotonicPass + "\n");
            out.write("initial_maximum_stress_Pa," + initialStress + "," + initialStressLimit + "," + initialStressPass + "\n");
            out.write("initial_zero_stress_applicable," + initialStressApplicable + ",1," + initialStressApplicable + "\n");
            out.write("negative_capacity_ratio," + negativeCapacityRatio + ",1.0," + negativeCapacityPass + "\n");
            out.write("quantitative_ready," + quantitativeReady + ",1," + quantitativeReady + "\n");
            out.write("voltage_rmse_V," + curveValidation.voltageRmse() + "," + activeVoltageRmseLimit
                    + "," + (curveValidation.applicable() && curveValidation.voltageRmse() <= activeVoltageRmseLimit) + "\n");
            out.write("voltage_rmse_publication_target_V," + curveValidation.voltageRmse() + ","
                    + cell.voltageRmseLimit() + ","
                    + (curveValidation.applicable() && curveValidation.voltageRmse() <= cell.voltageRmseLimit()) + "\n");
            out.write("cutoff_capacity_relative_error," + curveValidation.capacityError() + "," + cell.capacityErrorLimit()
                    + "," + (curveValidation.applicable() && curveValidation.capacityError() <= cell.capacityErrorLimit()) + "\n");
            out.write("experimental_curve_applicable," + curveValidation.applicable() + ",1,"
                    + curveValidation.applicable() + "\n");
        }
        return new ExportResult(metricsFile, timeSeries, radial, acceptance,
                peakStress, maximumAverageStress, stressP95, finalAverageX, maximumConcentrationDelta,
                massError, massPass, quantitativeReady);
    }

    private boolean monotonic(double[] values, boolean decreasing) {
        double tolerance = 1e-7;
        for (int i = 1; i < values.length; i++) {
            if (decreasing && values[i] > values[i-1] + tolerance) return false;
            if (!decreasing && values[i] < values[i-1] - tolerance) return false;
        }
        return values.length > 1;
    }

    private double parseLeadingDouble(String expression) {
        int bracket = expression.indexOf('[');
        return Double.parseDouble((bracket < 0 ? expression : expression.substring(0, bracket)).trim());
    }

    /** Compares a 0.1C charge solution against the configured capacity-voltage CSV. */
    private CurveValidation validateExperimentalCurve(Path file, double[] simulatedCapacity,
                                                      double[] simulatedVoltage) throws IOException {
        if (simulatedCapacity.length != simulatedVoltage.length || simulatedCapacity.length < 2) {
            throw new IOException("Capacity and voltage series have incompatible solution levels");
        }
        List<CurvePoint> experiment = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String stripped = line.strip();
            if (stripped.isEmpty() || stripped.startsWith("#")) continue;
            String[] columns = stripped.split("[,;\\t]");
            if (columns.length < 2) continue;
            try {
                experiment.add(new CurvePoint(Double.parseDouble(columns[0].trim()),
                        Double.parseDouble(columns[1].trim())));
            } catch (NumberFormatException ignored) {
                // Header row.
            }
        }
        experiment.sort(Comparator.comparingDouble(CurvePoint::capacity));
        if (experiment.size() < 2) throw new IOException("Experimental curve needs at least two numeric rows: " + file);

        double minCapacity = Math.min(simulatedCapacity[0], simulatedCapacity[simulatedCapacity.length - 1]);
        double maxCapacity = Math.max(simulatedCapacity[0], simulatedCapacity[simulatedCapacity.length - 1]);
        double squaredError = 0.0;
        int count = 0;
        for (CurvePoint point : experiment) {
            if (point.capacity() < minCapacity || point.capacity() > maxCapacity) continue;
            double interpolated = interpolate(simulatedCapacity, simulatedVoltage, point.capacity());
            squaredError += Math.pow(interpolated - point.voltage(), 2);
            count++;
        }
        if (count < 2) throw new IOException("Experimental and simulated capacity ranges do not overlap sufficiently: " + file);
        double rmse = Math.sqrt(squaredError / count);
        double experimentalCutoffCapacity = experiment.get(experiment.size() - 1).capacity();
        double simulatedCutoffCapacity = simulatedCapacity[simulatedCapacity.length - 1];
        double capacityError = Math.abs(simulatedCutoffCapacity - experimentalCutoffCapacity)
                / Math.max(Math.abs(experimentalCutoffCapacity), 1e-30);
        return new CurveValidation(true, rmse, capacityError);
    }

    private double interpolate(double[] x, double[] y, double target) {
        for (int i = 1; i < x.length; i++) {
            double lo = Math.min(x[i - 1], x[i]);
            double hi = Math.max(x[i - 1], x[i]);
            if (target < lo || target > hi || x[i] == x[i - 1]) continue;
            double fraction = (target - x[i - 1]) / (x[i] - x[i - 1]);
            return y[i - 1] + fraction * (y[i] - y[i - 1]);
        }
        return y[target <= x[0] ? 0 : y.length - 1];
    }

    /** Node-sampled diagnostic percentile; mean stress remains the volume-weighted primary metric. */
    private double exportAndCalculateStressP95(Model model, String dataset, String id,
                                               int solutionLevels, Path file) throws IOException {
        String tag = "stress_samples_" + id;
        model.result().export().create(tag, "Data");
        var data = model.result().export(tag);
        data.set("data", dataset);
        data.selection().named(ComsolTagUtils.POSITIVE_PARTICLE);
        data.set("expr", new String[]{"solid_full.mises"});
        data.set("unit", new String[]{"Pa"});
        data.set("looplevelinput", "manual");
        data.set("looplevel", new int[]{solutionLevels});
        data.set("filename", PathUtils.comsolPath(file));
        data.run();

        List<Double> samples = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String stripped = line.strip();
            if (stripped.isEmpty() || stripped.startsWith("%")) continue;
            String[] columns = stripped.split("[,;\\s]+");
            try {
                double value = Double.parseDouble(columns[columns.length-1]);
                if (Double.isFinite(value)) samples.add(value);
            } catch (NumberFormatException ignored) {
                // COMSOL may emit a textual header depending on export preferences.
            }
        }
        if (samples.isEmpty()) throw new IOException("No stress samples exported for percentile: " + file);
        samples.sort(Double::compareTo);
        int index = Math.max(0, (int)Math.ceil(0.95*samples.size())-1);
        return samples.get(index);
    }

    private void exportVoltageFigures(Model model, Path directory, String stem, String id,
                                      double[] voltages, List<Double> targets, Path indexFile) throws IOException {
        try (BufferedWriter out = Files.newBufferedWriter(indexFile, StandardCharsets.UTF_8)) {
            out.write("target_voltage_V,actual_voltage_V,solution_level,absolute_error_V\n");
            for (double target : targets) {
                int zeroBased = nearest(voltages, target);
                int level = zeroBased + 1;
                double actual = voltages[zeroBased];
                String voltage = Double.toString(target).replace('.', 'p');
                exportImage(model, "full_concentration", "img_conc_" + id + "_" + voltage, level,
                        directory.resolve(stem + "_concentration_V" + voltage + ".png"));
                exportImage(model, "full_stress", "img_stress_" + id + "_" + voltage, level,
                        directory.resolve(stem + "_stress_V" + voltage + ".png"));
                exportImage(model, "full_electrolyte", "img_el_" + id + "_" + voltage, level,
                        directory.resolve(stem + "_electrolyte_concentration_V" + voltage + ".png"));
                exportImage(model, "full_electrolyte_potential", "img_phil_" + id + "_" + voltage, level,
                        directory.resolve(stem + "_electrolyte_potential_V" + voltage + ".png"));
                exportImage(model, "full_solid_potential", "img_phis_" + id + "_" + voltage, level,
                        directory.resolve(stem + "_solid_potential_V" + voltage + ".png"));
                out.write(target + "," + actual + "," + level + "," + Math.abs(actual-target) + "\n");
            }
        }
    }

    private void exportSocFigures(Model model, Path directory, String stem, String id,
                                  double[] averageX, MaterialConfig material,
                                  List<Double> targets, Path indexFile) throws IOException {
        try (BufferedWriter out = Files.newBufferedWriter(indexFile, StandardCharsets.UTF_8)) {
            out.write("target_desodiation_fraction,target_average_xNa,actual_average_xNa,solution_level,absolute_xNa_error\n");
            for (double target : targets) {
                double targetX = material.initialX()-target*(material.initialX()-material.finalChargeX());
                int zeroBased = nearest(averageX, targetX);
                int level = zeroBased+1;
                String soc = Double.toString(target).replace('.', 'p');
                exportImage(model, "full_concentration", "img_conc_soc_" + id + "_" + soc, level,
                        directory.resolve(stem + "_concentration_SOC" + soc + ".png"));
                exportImage(model, "full_stress", "img_stress_soc_" + id + "_" + soc, level,
                        directory.resolve(stem + "_stress_SOC" + soc + ".png"));
                out.write(target + "," + targetX + "," + averageX[zeroBased] + "," + level + ","
                        + Math.abs(averageX[zeroBased]-targetX) + "\n");
            }
        }
    }

    private void exportImage(Model model, String plot, String tag, int level, Path file) {
        model.result(plot).set("looplevelinput", "manual");
        model.result(plot).set("looplevel", new int[]{level});
        model.result().export().create(tag, "Image3D");
        var image = model.result().export(tag);
        image.set("plotgroup", plot);
        image.set("pngfilename", PathUtils.comsolPath(file));
        image.set("width", 1400);
        image.set("height", 1000);
        image.set("resolution", 150);
        image.run();
    }

    private int nearest(double[] values, double target) {
        int best = 0;
        double distance = Double.POSITIVE_INFINITY;
        for (int i = 0; i < values.length; i++) {
            double d = Math.abs(values[i] - target);
            if (d < distance) { best = i; distance = d; }
        }
        return best;
    }

    private int maximumIndex(double[] values) {
        if (values.length == 0) return 0;
        int best = 0;
        for (int i = 1; i < values.length; i++) if (values[i] > values[best]) best = i;
        return best;
    }

    private double[] series(Model model, String tag, String dataset, String expression) {
        model.result().numerical().create(tag, "EvalGlobal");
        var numerical = model.result().numerical(tag);
        numerical.set("data", dataset);
        numerical.set("expr", new String[]{expression});
        numerical.set("looplevelinput", "all");
        double[][] matrix = numerical.getReal();
        List<Double> values = new ArrayList<>();
        for (double[] row : matrix) for (double value : row) values.add(value);
        return values.stream().mapToDouble(Double::doubleValue).toArray();
    }

    private void writeMetrics(Path file, List<String> names, List<Double> values) throws IOException {
        try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            out.write(String.join(",", names)); out.newLine();
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) out.write(',');
                out.write(Double.toString(values.get(i)));
            }
            out.newLine();
        }
    }

    public record ExportResult(Path metrics, Path timeSeries, Path radial, Path acceptance,
                               double maximumStress, double averageStress, double stressP95,
                               double averageX, double concentrationDelta, double massBalanceError,
                               boolean massBalancePass, boolean quantitativeReady) {}

    private record CurvePoint(double capacity, double voltage) {}

    private record CurveValidation(boolean applicable, double voltageRmse, double capacityError) {
        static CurveValidation notApplicable() {
            return new CurveValidation(false, Double.NaN, Double.NaN);
        }
    }
}
