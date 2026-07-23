package com.nfm.comsol.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/** Global solver/export controls compatible with the COMSOL Java compiler. */
public final class SimulationConfig {
    private final String comsolVersion;
    private final List<Double> cRates;
    private final String defaultMode, dischargeInitialization, meshLevel, meshMaxSize, meshMinSize;
    private final double meshGrowthRate, meshConvergenceTolerance, relativeTolerance;
    private final List<Double> outputFractions;
    private final double maxStepFraction;
    private final String smokeDuration, smokeMeshSize;
    private final Path outputRoot;
    private final double concentrationScaleMin, concentrationScaleMax;
    private final String stressScaleMin, stressScaleMax;
    private final List<Double> exportFractions;
    private final String initialStressTolerance;
    private final Path projectRoot, sourceFile;
    private final Properties snapshot;

    public SimulationConfig(
            String comsolVersion, List<Double> cRates, String defaultMode,
            String dischargeInitialization, String meshLevel, String meshMaxSize,
            String meshMinSize, double meshGrowthRate, double meshConvergenceTolerance,
            double relativeTolerance, List<Double> outputFractions, double maxStepFraction,
            String smokeDuration, String smokeMeshSize, Path outputRoot,
            double concentrationScaleMin, double concentrationScaleMax,
            String stressScaleMin, String stressScaleMax, List<Double> exportFractions,
            String initialStressTolerance, Path projectRoot, Path sourceFile,
            Properties snapshot) {
        this.comsolVersion = comsolVersion;
        this.cRates = cRates;
        this.defaultMode = defaultMode;
        this.dischargeInitialization = dischargeInitialization;
        this.meshLevel = meshLevel;
        this.meshMaxSize = meshMaxSize;
        this.meshMinSize = meshMinSize;
        this.meshGrowthRate = meshGrowthRate;
        this.meshConvergenceTolerance = meshConvergenceTolerance;
        this.relativeTolerance = relativeTolerance;
        this.outputFractions = outputFractions;
        this.maxStepFraction = maxStepFraction;
        this.smokeDuration = smokeDuration;
        this.smokeMeshSize = smokeMeshSize;
        this.outputRoot = outputRoot;
        this.concentrationScaleMin = concentrationScaleMin;
        this.concentrationScaleMax = concentrationScaleMax;
        this.stressScaleMin = stressScaleMin;
        this.stressScaleMax = stressScaleMax;
        this.exportFractions = exportFractions;
        this.initialStressTolerance = initialStressTolerance;
        this.projectRoot = projectRoot;
        this.sourceFile = sourceFile;
        this.snapshot = snapshot;
    }

    public String comsolVersion() { return comsolVersion; }
    public List<Double> cRates() { return cRates; }
    public String defaultMode() { return defaultMode; }
    public String dischargeInitialization() { return dischargeInitialization; }
    public String meshLevel() { return meshLevel; }
    public String meshMaxSize() { return meshMaxSize; }
    public String meshMinSize() { return meshMinSize; }
    public double meshGrowthRate() { return meshGrowthRate; }
    public double meshConvergenceTolerance() { return meshConvergenceTolerance; }
    public double relativeTolerance() { return relativeTolerance; }
    public List<Double> outputFractions() { return outputFractions; }
    public double maxStepFraction() { return maxStepFraction; }
    public String smokeDuration() { return smokeDuration; }
    public String smokeMeshSize() { return smokeMeshSize; }
    public Path outputRoot() { return outputRoot; }
    public double concentrationScaleMin() { return concentrationScaleMin; }
    public double concentrationScaleMax() { return concentrationScaleMax; }
    public String stressScaleMin() { return stressScaleMin; }
    public String stressScaleMax() { return stressScaleMax; }
    public List<Double> exportFractions() { return exportFractions; }
    public String initialStressTolerance() { return initialStressTolerance; }
    public Path projectRoot() { return projectRoot; }
    public Path sourceFile() { return sourceFile; }
    public Properties snapshot() { return snapshot; }

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
