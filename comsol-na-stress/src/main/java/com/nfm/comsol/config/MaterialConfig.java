package com.nfm.comsol.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/** Immutable material configuration; string-valued SI expressions retain COMSOL units. */
public record MaterialConfig(
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

    public String diffusivityFor(String mode) {
        return mode.equals("discharge") ? dischargeDiffusivity : chargeDiffusivity;
    }
}
