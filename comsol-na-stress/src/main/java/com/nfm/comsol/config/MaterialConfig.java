package com.nfm.comsol.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/** Immutable material configuration compatible with the COMSOL Java compiler. */
public final class MaterialConfig {
    private final String name, formula, radius, density, capacity, molarMass, csmax;
    private final double initialX, finalChargeX;
    private final String chargeDiffusivity, dischargeDiffusivity, youngModulus;
    private final List<Double> youngModulusSensitivityGpa;
    private final double poissonRatio;
    private final List<Double> poissonSensitivityValues;
    private final double beta;
    private final List<Double> betaSensitivityValues;
    private final String parameterStatus, parameterSource;
    private final double parameterUncertainty;
    private final Path parameterMetadataCsv, ocvCsv, experimentalCurveCsv;
    private final String exchangeCurrentDensity;
    private final List<Double> exchangeCurrentDensitySensitivity;
    private final String diffusionMode;
    private final Path chargeDiffusionCsv, dischargeDiffusionCsv;
    private final String strainMode;
    private final Path strainCsv;
    private final boolean phaseEnabled;
    private final double phaseHigh, phaseLow, phaseExtraStrain, phaseSmoothingWidth;
    private final boolean gradientEnabled;
    private final double gradientExponent;
    private final String gradientDsCore, gradientDsSurface;
    private final double gradientBetaCore, gradientBetaSurface;
    private final Path sourceFile;
    private final Properties snapshot;

    public MaterialConfig(
            String name, String formula, String radius, String density, String capacity,
            String molarMass, String csmax, double initialX, double finalChargeX,
            String chargeDiffusivity, String dischargeDiffusivity,
            String youngModulus, List<Double> youngModulusSensitivityGpa,
            double poissonRatio, List<Double> poissonSensitivityValues,
            double beta, List<Double> betaSensitivityValues, String parameterStatus,
            String parameterSource, double parameterUncertainty, Path parameterMetadataCsv,
            Path ocvCsv, Path experimentalCurveCsv, String exchangeCurrentDensity,
            List<Double> exchangeCurrentDensitySensitivity,
            String diffusionMode, Path chargeDiffusionCsv, Path dischargeDiffusionCsv,
            String strainMode, Path strainCsv,
            boolean phaseEnabled, double phaseHigh, double phaseLow, double phaseExtraStrain,
            double phaseSmoothingWidth, boolean gradientEnabled, double gradientExponent,
            String gradientDsCore, String gradientDsSurface, double gradientBetaCore,
            double gradientBetaSurface, Path sourceFile, Properties snapshot) {
        this.name = name;
        this.formula = formula;
        this.radius = radius;
        this.density = density;
        this.capacity = capacity;
        this.molarMass = molarMass;
        this.csmax = csmax;
        this.initialX = initialX;
        this.finalChargeX = finalChargeX;
        this.chargeDiffusivity = chargeDiffusivity;
        this.dischargeDiffusivity = dischargeDiffusivity;
        this.youngModulus = youngModulus;
        this.youngModulusSensitivityGpa = youngModulusSensitivityGpa;
        this.poissonRatio = poissonRatio;
        this.poissonSensitivityValues = poissonSensitivityValues;
        this.beta = beta;
        this.betaSensitivityValues = betaSensitivityValues;
        this.parameterStatus = parameterStatus;
        this.parameterSource = parameterSource;
        this.parameterUncertainty = parameterUncertainty;
        this.parameterMetadataCsv = parameterMetadataCsv;
        this.ocvCsv = ocvCsv;
        this.experimentalCurveCsv = experimentalCurveCsv;
        this.exchangeCurrentDensity = exchangeCurrentDensity;
        this.exchangeCurrentDensitySensitivity = exchangeCurrentDensitySensitivity;
        this.diffusionMode = diffusionMode;
        this.chargeDiffusionCsv = chargeDiffusionCsv;
        this.dischargeDiffusionCsv = dischargeDiffusionCsv;
        this.strainMode = strainMode;
        this.strainCsv = strainCsv;
        this.phaseEnabled = phaseEnabled;
        this.phaseHigh = phaseHigh;
        this.phaseLow = phaseLow;
        this.phaseExtraStrain = phaseExtraStrain;
        this.phaseSmoothingWidth = phaseSmoothingWidth;
        this.gradientEnabled = gradientEnabled;
        this.gradientExponent = gradientExponent;
        this.gradientDsCore = gradientDsCore;
        this.gradientDsSurface = gradientDsSurface;
        this.gradientBetaCore = gradientBetaCore;
        this.gradientBetaSurface = gradientBetaSurface;
        this.sourceFile = sourceFile;
        this.snapshot = snapshot;
    }

    public String name() { return name; }
    public String formula() { return formula; }
    public String radius() { return radius; }
    public String density() { return density; }
    public String capacity() { return capacity; }
    public String molarMass() { return molarMass; }
    public String csmax() { return csmax; }
    public double initialX() { return initialX; }
    public double finalChargeX() { return finalChargeX; }
    public String chargeDiffusivity() { return chargeDiffusivity; }
    public String dischargeDiffusivity() { return dischargeDiffusivity; }
    public String youngModulus() { return youngModulus; }
    public List<Double> youngModulusSensitivityGpa() { return youngModulusSensitivityGpa; }
    public double poissonRatio() { return poissonRatio; }
    public List<Double> poissonSensitivityValues() { return poissonSensitivityValues; }
    public double beta() { return beta; }
    public List<Double> betaSensitivityValues() { return betaSensitivityValues; }
    public String parameterStatus() { return parameterStatus; }
    public String parameterSource() { return parameterSource; }
    public double parameterUncertainty() { return parameterUncertainty; }
    public Path parameterMetadataCsv() { return parameterMetadataCsv; }
    public Path ocvCsv() { return ocvCsv; }
    public Path experimentalCurveCsv() { return experimentalCurveCsv; }
    public String exchangeCurrentDensity() { return exchangeCurrentDensity; }
    public List<Double> exchangeCurrentDensitySensitivity() { return exchangeCurrentDensitySensitivity; }
    public String diffusionMode() { return diffusionMode; }
    public Path chargeDiffusionCsv() { return chargeDiffusionCsv; }
    public Path dischargeDiffusionCsv() { return dischargeDiffusionCsv; }
    public String strainMode() { return strainMode; }
    public Path strainCsv() { return strainCsv; }
    public boolean phaseEnabled() { return phaseEnabled; }
    public double phaseHigh() { return phaseHigh; }
    public double phaseLow() { return phaseLow; }
    public double phaseExtraStrain() { return phaseExtraStrain; }
    public double phaseSmoothingWidth() { return phaseSmoothingWidth; }
    public boolean gradientEnabled() { return gradientEnabled; }
    public double gradientExponent() { return gradientExponent; }
    public String gradientDsCore() { return gradientDsCore; }
    public String gradientDsSurface() { return gradientDsSurface; }
    public double gradientBetaCore() { return gradientBetaCore; }
    public double gradientBetaSurface() { return gradientBetaSurface; }
    public Path sourceFile() { return sourceFile; }
    public Properties snapshot() { return snapshot; }

    public String diffusivityFor(String mode) {
        return mode.equals("discharge") ? dischargeDiffusivity : chargeDiffusivity;
    }
}
