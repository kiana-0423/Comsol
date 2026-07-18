package com.nfm.comsol.config;

import java.nio.file.Path;
import java.util.Properties;

/** Immutable material configuration; string-valued SI expressions retain COMSOL units. */
public record MaterialConfig(
        String name, String formula, String radius, String density, String capacity,
        String csmax, double initialX, double finalChargeX, String diffusivity,
        String youngModulus, double poissonRatio, double beta, String parameterStatus,
        String diffusionMode, Path diffusionCsv, String strainMode, Path strainCsv,
        boolean phaseEnabled, double phaseStart, double phaseEnd, double phaseExtraStrain,
        double phaseSmoothingWidth, boolean gradientEnabled, double gradientExponent,
        String gradientDsCore, String gradientDsSurface, double gradientBetaCore,
        double gradientBetaSurface, Path sourceFile, Properties snapshot) {
}
