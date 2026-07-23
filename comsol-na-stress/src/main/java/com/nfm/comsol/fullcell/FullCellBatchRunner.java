package com.nfm.comsol.fullcell;

import com.nfm.comsol.config.ConfigLoader;
import com.nfm.comsol.config.FullCellConfig;
import com.nfm.comsol.config.MaterialConfig;
import com.nfm.comsol.config.SimulationConfig;
import com.nfm.comsol.util.PathUtils;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Full-cell matrix, convergence and one-at-a-time uncertainty studies. */
public final class FullCellBatchRunner {
    private final FullCellSimulationRunner runner = new FullCellSimulationRunner();
    private final ComparisonFigureComposer comparisonFigures = new ComparisonFigureComposer();

    public Path runAll(FullCellConfig cell, SimulationConfig simulation,
                       FullCellSimulationRunner.RunOptions base) throws Exception {
        List<FullCellSimulationRunner.RunResult> results = new ArrayList<>();
        for (String name : Arrays.asList("NFM", "NFMZC")) {
            MaterialConfig material = ConfigLoader.loadMaterial(simulation.projectRoot(), name);
            for (double rate : simulation.cRates()) {
                results.add(runner.run(material, cell, simulation,
                        new FullCellSimulationRunner.RunOptions(rate, base.mode(), base.buildOnly(), base.smokeTest()),
                        SensitivityCase.baseline()));
            }
        }
        Path summary = simulation.outputRoot().resolve("csv").resolve("FULLCELL_NFM_NFMZC_summary.csv");
        writeSummary(summary, results);
        if (!base.buildOnly() && !base.smokeTest()) {
            for (double rate : simulation.cRates()) comparisonFigures.compose(cell, simulation, rate, base.mode());
        }
        return summary;
    }

    public Path runMeshConvergence(MaterialConfig material, FullCellConfig cell,
                                   SimulationConfig simulation, double cRate, String mode) throws Exception {
        List<FullCellSimulationRunner.RunResult> results = new ArrayList<>();
        for (String level : Arrays.asList("normal", "fine", "extra_fine")) {
            results.add(runner.run(material, cell, simulation.withMeshLevel(level),
                    new FullCellSimulationRunner.RunOptions(cRate, mode, false, false),
                    new SensitivityCase("mesh_" + level, 1, 1, 1, 1, 1, 1, 1, 1)));
        }
        Path file = simulation.outputRoot().resolve("csv").resolve(
                "FULLCELL_" + PathUtils.caseStem(material.name(), mode, cRate) + "_mesh_convergence.csv");
        try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            out.write("case,element_count,final_average_xNa,maximum_concentration_delta_xNa,maximum_average_stress_Pa,stress_p95_Pa,maximum_stress_Pa,average_xNa_change,concentration_delta_change,average_stress_change,stress_p95_change,maximum_stress_change,converged\n");
            FullCellSimulationRunner.RunResult previous = null;
            for (FullCellSimulationRunner.RunResult r : results) {
                double xChange = previous == null ? Double.NaN : relative(r.finalAverageX(), previous.finalAverageX());
                double deltaChange = previous == null ? Double.NaN : relative(r.maximumConcentrationDelta(), previous.maximumConcentrationDelta());
                double avgStressChange = previous == null ? Double.NaN : relative(r.maximumAverageStress(), previous.maximumAverageStress());
                double p95Change = previous == null ? Double.NaN : relative(r.stressP95(), previous.stressP95());
                double maxChange = previous == null ? Double.NaN : relative(r.maximumStress(), previous.maximumStress());
                boolean converged = previous != null
                        && xChange < cell.convergenceAverageConcentrationLimit()
                        && deltaChange < cell.convergenceConcentrationDeltaLimit()
                        && avgStressChange < cell.convergenceAverageStressLimit()
                        && p95Change < cell.convergenceStressP95Limit();
                out.write(r.stem() + "," + r.elementCount() + "," + r.finalAverageX() + ","
                        + r.maximumConcentrationDelta() + "," + r.maximumAverageStress() + ","
                        + r.stressP95() + "," + r.maximumStress() + "," + xChange + ","
                        + deltaChange + "," + avgStressChange + "," + p95Change + ","
                        + maxChange + "," + converged + "\n");
                previous = r;
            }
        }
        return file;
    }

    public Path runSensitivity(MaterialConfig material, FullCellConfig cell,
                               SimulationConfig simulation, double cRate, String mode) throws Exception {
        double u = Math.max(material.parameterUncertainty(), cell.parameterUncertainty());
        double low = Math.max(0.05, 1-u);
        double high = 1+u;
        double radiusLow = Math.max(0.85, low);
        double radiusHigh = Math.min(1.15, high);
        double strainLowScale = material.betaSensitivityValues().get(0) / material.betaSensitivityValues().get(1);
        double strainHighScale = material.betaSensitivityValues().get(2) / material.betaSensitivityValues().get(1);
        double modulusLowScale = material.youngModulusSensitivityGpa().get(0) / material.youngModulusSensitivityGpa().get(1);
        double modulusHighScale = material.youngModulusSensitivityGpa().get(2) / material.youngModulusSensitivityGpa().get(1);
        double poissonLowScale = material.poissonSensitivityValues().get(0) / material.poissonRatio();
        double poissonHighScale = material.poissonSensitivityValues().get(2) / material.poissonRatio();
        double positiveKineticsLowScale = material.exchangeCurrentDensitySensitivity().get(0)
                / material.exchangeCurrentDensitySensitivity().get(1);
        double positiveKineticsHighScale = material.exchangeCurrentDensitySensitivity().get(2)
                / material.exchangeCurrentDensitySensitivity().get(1);
        double negativeKineticsLowScale = cell.negativeExchangeCurrentDensitySensitivity().get(0)
                / cell.negativeExchangeCurrentDensitySensitivity().get(1);
        double negativeKineticsHighScale = cell.negativeExchangeCurrentDensitySensitivity().get(2)
                / cell.negativeExchangeCurrentDensitySensitivity().get(1);
        List<SensitivityCase> cases = Arrays.asList(
                SensitivityCase.baseline(),
                new SensitivityCase("diffusion_low", low,low,1,1,1,1,1,1),
                new SensitivityCase("diffusion_high", high,high,1,1,1,1,1,1),
                new SensitivityCase("strain_low", 1,1,strainLowScale,1,1,1,1,1),
                new SensitivityCase("strain_high", 1,1,strainHighScale,1,1,1,1,1),
                new SensitivityCase("modulus_low", 1,1,1,modulusLowScale,1,1,1,1),
                new SensitivityCase("modulus_high", 1,1,1,modulusHighScale,1,1,1,1),
                new SensitivityCase("poisson_low", 1,1,1,1,poissonLowScale,1,1,1),
                new SensitivityCase("poisson_high", 1,1,1,1,poissonHighScale,1,1,1),
                new SensitivityCase("radius_low", 1,1,1,1,1,radiusLow,1,1),
                new SensitivityCase("radius_high", 1,1,1,1,1,radiusHigh,1,1),
                new SensitivityCase("positive_kinetics_low", 1,1,1,1,1,1,positiveKineticsLowScale,1),
                new SensitivityCase("positive_kinetics_high", 1,1,1,1,1,1,positiveKineticsHighScale,1),
                new SensitivityCase("negative_kinetics_low", 1,1,1,1,1,1,1,negativeKineticsLowScale),
                new SensitivityCase("negative_kinetics_high", 1,1,1,1,1,1,1,negativeKineticsHighScale));
        List<FullCellSimulationRunner.RunResult> results = new ArrayList<>();
        for (SensitivityCase sensitivity : cases) {
            results.add(runner.run(material, cell, simulation,
                    new FullCellSimulationRunner.RunOptions(cRate, mode, false, false), sensitivity));
        }
        Path file = simulation.outputRoot().resolve("csv").resolve(
                "FULLCELL_" + PathUtils.caseStem(material.name(), mode, cRate) + "_sensitivity.csv");
        writeSummary(file, results);
        Path interval = simulation.outputRoot().resolve("csv").resolve(
                "FULLCELL_" + PathUtils.caseStem(material.name(), mode, cRate) + "_stress_uncertainty.csv");
        double baseline = results.get(0).maximumStress();
        double minimum = results.stream().mapToDouble(FullCellSimulationRunner.RunResult::maximumStress).min().orElse(Double.NaN);
        double maximum = results.stream().mapToDouble(FullCellSimulationRunner.RunResult::maximumStress).max().orElse(Double.NaN);
        try (BufferedWriter out = Files.newBufferedWriter(interval, StandardCharsets.UTF_8)) {
            out.write("baseline_Pa,minimum_Pa,maximum_Pa,lower_relative,upper_relative,quantitative_ready\n");
            out.write(baseline + "," + minimum + "," + maximum + ","
                    + (baseline-minimum)/Math.max(Math.abs(baseline),1e-30) + ","
                    + (maximum-baseline)/Math.max(Math.abs(baseline),1e-30) + ","
                    + results.stream().allMatch(FullCellSimulationRunner.RunResult::quantitativeReady) + "\n");
        }
        return file;
    }

    /** Separates diffusion and mechanics/strain contributions on a common NFM baseline. */
    public Path runAttribution(FullCellConfig cell, SimulationConfig simulation,
                               double cRate, String mode) throws Exception {
        MaterialConfig nfm = ConfigLoader.loadMaterial(simulation.projectRoot(), "NFM");
        MaterialConfig nfmzc = ConfigLoader.loadMaterial(simulation.projectRoot(), "NFMZC");
        double chargeRatio = leading(nfmzc.chargeDiffusivity())/leading(nfm.chargeDiffusivity());
        double dischargeRatio = leading(nfmzc.dischargeDiffusivity())/leading(nfm.dischargeDiffusivity());
        double strainRatio = nfmzc.beta()/Math.max(Math.abs(nfm.beta()),1e-30);
        double modulusRatio = leading(nfmzc.youngModulus())/leading(nfm.youngModulus());
        double poissonRatio = nfmzc.poissonRatio()/Math.max(Math.abs(nfm.poissonRatio()),1e-30);
        double positiveKineticsRatio = leading(nfmzc.exchangeCurrentDensity())
                / leading(nfm.exchangeCurrentDensity());

        List<FullCellSimulationRunner.RunResult> results = new ArrayList<>();
        results.add(runner.run(nfm, cell, simulation,
                new FullCellSimulationRunner.RunOptions(cRate, mode, false, false),
                new SensitivityCase("actual_nfm",1,1,1,1,1,1,1,1)));
        results.add(runner.run(nfmzc, cell, simulation,
                new FullCellSimulationRunner.RunOptions(cRate, mode, false, false),
                new SensitivityCase("actual_nfmzc",1,1,1,1,1,1,1,1)));
        results.add(runner.run(nfm, cell, simulation,
                new FullCellSimulationRunner.RunOptions(cRate, mode, false, false),
                new SensitivityCase("nfm_with_nfmzc_diffusion",
                        chargeRatio,dischargeRatio,1,1,1,1,1,1)));
        results.add(runner.run(nfm, cell, simulation,
                new FullCellSimulationRunner.RunOptions(cRate, mode, false, false),
                new SensitivityCase("nfm_with_nfmzc_strain_only",
                        1,1,strainRatio,1,1,1,1,1)));
        results.add(runner.run(nfm, cell, simulation,
                new FullCellSimulationRunner.RunOptions(cRate, mode, false, false),
                new SensitivityCase("nfm_with_nfmzc_modulus_only",
                        1,1,1,modulusRatio,1,1,1,1)));
        results.add(runner.run(nfm, cell, simulation,
                new FullCellSimulationRunner.RunOptions(cRate, mode, false, false),
                new SensitivityCase("nfm_with_nfmzc_positive_kinetics",
                        1,1,1,1,1,1,positiveKineticsRatio,1)));
        results.add(runner.run(nfm, cell, simulation,
                new FullCellSimulationRunner.RunOptions(cRate, mode, false, false),
                new SensitivityCase("nfm_with_nfmzc_mechanics",
                        1,1,strainRatio,modulusRatio,poissonRatio,1,1,1)));
        results.add(runner.run(nfm, cell, simulation,
                new FullCellSimulationRunner.RunOptions(cRate, mode, false, false),
                new SensitivityCase("nfm_with_nfmzc_all_configured_differences",
                        chargeRatio,dischargeRatio,strainRatio,modulusRatio,poissonRatio,1,
                        positiveKineticsRatio,1)));
        Path file = simulation.outputRoot().resolve("csv").resolve(
                "FULLCELL_" + PathUtils.caseStem("ATTRIBUTION", mode, cRate) + ".csv");
        writeSummary(file, results);
        return file;
    }

    public Path runTimeConvergence(MaterialConfig material, FullCellConfig cell,
                                   SimulationConfig simulation, double cRate, String mode) throws Exception {
        List<Double> fractions = Arrays.asList(0.04, 0.02, 0.01);
        List<FullCellSimulationRunner.RunResult> results = new ArrayList<>();
        for (double fraction : fractions) {
            String id = Double.toString(fraction).replace('.', 'p');
            results.add(runner.run(material, cell, simulation.withMaxStepFraction(fraction),
                    new FullCellSimulationRunner.RunOptions(cRate, mode, false, false),
                    new SensitivityCase("dt_" + id,1,1,1,1,1,1,1,1)));
        }
        Path file = simulation.outputRoot().resolve("csv").resolve(
                "FULLCELL_" + PathUtils.caseStem(material.name(), mode, cRate) + "_time_convergence.csv");
        try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            out.write("max_step_fraction,final_average_xNa,maximum_concentration_delta_xNa,maximum_average_stress_Pa,stress_p95_Pa,maximum_stress_Pa,average_xNa_change,concentration_delta_change,average_stress_change,stress_p95_change,maximum_stress_change,converged\n");
            FullCellSimulationRunner.RunResult previous = null;
            for (int i = 0; i < results.size(); i++) {
                FullCellSimulationRunner.RunResult r = results.get(i);
                double xChange = previous == null ? Double.NaN : relative(r.finalAverageX(), previous.finalAverageX());
                double deltaChange = previous == null ? Double.NaN : relative(r.maximumConcentrationDelta(), previous.maximumConcentrationDelta());
                double avgStressChange = previous == null ? Double.NaN : relative(r.maximumAverageStress(), previous.maximumAverageStress());
                double p95Change = previous == null ? Double.NaN : relative(r.stressP95(), previous.stressP95());
                double maxChange = previous == null ? Double.NaN : relative(r.maximumStress(), previous.maximumStress());
                boolean converged = previous != null
                        && xChange < cell.convergenceAverageConcentrationLimit()
                        && deltaChange < cell.convergenceConcentrationDeltaLimit()
                        && avgStressChange < cell.convergenceAverageStressLimit()
                        && p95Change < cell.convergenceStressP95Limit();
                out.write(fractions.get(i) + "," + r.finalAverageX() + "," + r.maximumConcentrationDelta() + ","
                        + r.maximumAverageStress() + "," + r.stressP95() + "," + r.maximumStress() + ","
                        + xChange + "," + deltaChange + "," + avgStressChange + "," + p95Change + ","
                        + maxChange + "," + converged + "\n");
                previous = r;
            }
        }
        return file;
    }

    private void writeSummary(Path file, List<FullCellSimulationRunner.RunResult> results) throws Exception {
        try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            out.write("case,maximum_von_mises_Pa,maximum_average_stress_Pa,stress_p95_Pa,final_average_xNa,maximum_concentration_delta_xNa,mass_balance_relative_error,element_count,quantitative_ready,mph,metrics\n");
            for (FullCellSimulationRunner.RunResult r : results) {
                out.write(r.stem() + "," + r.maximumStress() + "," + r.maximumAverageStress() + ","
                        + r.stressP95() + "," + r.finalAverageX() + "," + r.maximumConcentrationDelta() + ","
                        + r.massBalanceError() + "," + r.elementCount() + "," + r.quantitativeReady() + ","
                        + r.mph() + "," + r.metrics() + "\n");
            }
        }
    }

    private double relative(double current, double previous) {
        return Math.abs(current-previous)/Math.max(Math.abs(previous),1e-12);
    }

    private double leading(String expression) {
        int bracket = expression.indexOf('[');
        return Double.parseDouble((bracket < 0 ? expression : expression.substring(0, bracket)).trim());
    }
}
