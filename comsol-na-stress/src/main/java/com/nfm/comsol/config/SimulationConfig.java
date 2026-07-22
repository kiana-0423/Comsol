package com.nfm.comsol.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/** Global solver/export controls. Material-specific physics belongs in MaterialConfig. */
public record SimulationConfig(
        String comsolVersion, List<Double> cRates, String defaultMode,
        String dischargeInitialization, String meshLevel, String meshMaxSize,
        String meshMinSize, double meshGrowthRate, double meshConvergenceTolerance,
        double relativeTolerance, List<Double> outputFractions, double maxStepFraction,
        String smokeDuration, String smokeMeshSize, Path outputRoot,
        double concentrationScaleMin, double concentrationScaleMax,
        String stressScaleMin, String stressScaleMax, List<Double> exportFractions,
        String initialStressTolerance, Path projectRoot, Path sourceFile,
        Properties snapshot) {

    public SimulationConfig withMeshLevel(String level) {
        return new SimulationConfig(comsolVersion, cRates, defaultMode, dischargeInitialization,
                level, meshMaxSize, meshMinSize, meshGrowthRate, meshConvergenceTolerance,
                relativeTolerance, outputFractions, maxStepFraction, smokeDuration,
                smokeMeshSize, outputRoot, concentrationScaleMin, concentrationScaleMax,
                stressScaleMin, stressScaleMax, exportFractions, initialStressTolerance,
                projectRoot, sourceFile, snapshot);
    }

    public SimulationConfig withMaxStepFraction(double fraction) {
        return new SimulationConfig(comsolVersion, cRates, defaultMode, dischargeInitialization,
                meshLevel, meshMaxSize, meshMinSize, meshGrowthRate, meshConvergenceTolerance,
                relativeTolerance, outputFractions, fraction, smokeDuration,
                smokeMeshSize, outputRoot, concentrationScaleMin, concentrationScaleMax,
                stressScaleMin, stressScaleMax, exportFractions, initialStressTolerance,
                projectRoot, sourceFile, snapshot);
    }
}
