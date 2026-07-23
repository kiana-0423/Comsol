package com.nfm.comsol.util;

import com.comsol.model.Model;
import com.nfm.comsol.config.MaterialConfig;
import com.nfm.comsol.config.FullCellConfig;
import com.nfm.comsol.config.SimulationConfig;

import java.nio.file.Files;
import java.util.List;
import java.util.Arrays;

public final class ValidationUtils {
    private ValidationUtils() {}

    public static void validateMaterialConfig(MaterialConfig c) {
        require(c.initialX() >= 0 && c.initialX() <= 1, "initial.x must be in [0,1]");
        require(c.finalChargeX() >= 0 && c.finalChargeX() < c.initialX(), "final.x.charge must be >=0 and below initial.x");
        require(c.poissonRatio() > -1 && c.poissonRatio() < 0.5, "poisson.ratio must be between -1 and 0.5");
        require(c.poissonSensitivityValues().size() == 3,
                "poisson.sensitivity.values must contain low, baseline, high");
        require(c.poissonSensitivityValues().get(0) < c.poissonSensitivityValues().get(1)
                        && c.poissonSensitivityValues().get(1) < c.poissonSensitivityValues().get(2),
                "poisson.sensitivity.values must be strictly increasing");
        require(c.poissonSensitivityValues().stream().allMatch(v -> v > -1 && v < 0.5),
                "poisson sensitivity values must be between -1 and 0.5");
        require(Math.abs(c.poissonSensitivityValues().get(1) - c.poissonRatio()) < 1e-12,
                "middle poisson sensitivity value must equal poisson.ratio");
        require(c.beta() >= 0, "chemical.expansion.beta must be nonnegative");
        validatePositiveTriplet(c.youngModulusSensitivityGpa(), "young.modulus.sensitivity.gpa");
        validatePositiveTriplet(c.betaSensitivityValues(), "chemical.expansion.beta.sensitivity.values");
        validatePositiveTriplet(c.exchangeCurrentDensitySensitivity(),
                "exchange.current.density.sensitivity.a_m2");
        require(Math.abs(c.betaSensitivityValues().get(1) - c.beta()) < 1e-12,
                "middle beta sensitivity value must equal chemical.expansion.beta");
        require(Math.abs(c.youngModulusSensitivityGpa().get(1) - leading(c.youngModulus())) < 1e-12,
                "middle Young-modulus sensitivity value must equal young.modulus in GPa");
        require(Math.abs(c.exchangeCurrentDensitySensitivity().get(1)
                        - leading(c.exchangeCurrentDensity())) < 1e-12,
                "middle exchange-current sensitivity value must equal exchange.current.density in A/m2");
        require(Arrays.asList("constant", "interpolation").contains(c.diffusionMode()), "invalid diffusion.mode");
        require(Arrays.asList("linear", "interpolation", "phase_transition").contains(c.strainMode()), "invalid strain.mode");
        require(c.phaseSmoothingWidth() > 0, "phase.smoothing.width must be positive");
        require(c.phaseHigh() > c.phaseLow(), "phase.x.high must exceed phase.x.low for decreasing-x charge");
        require(c.gradientExponent() > 0, "gradient.exponent must be positive");
        require(Arrays.asList("provisional", "literature", "measured").contains(c.parameterStatus().toLowerCase()),
                "parameter.status must be provisional, literature, or measured");
        require(c.parameterUncertainty() >= 0 && c.parameterUncertainty() <= 1,
                "parameter.uncertainty must be in [0,1]");
        require(!c.parameterSource().trim().isEmpty(), "parameter.source must not be blank");
    }

    private static void validatePositiveTriplet(List<Double> values, String name) {
        require(values.size() == 3, name + " must contain low, baseline, high");
        require(values.get(0) > 0 && values.get(0) < values.get(1) && values.get(1) < values.get(2),
                name + " must be positive and strictly increasing");
    }

    private static double leading(String expression) {
        int bracket = expression.indexOf('[');
        return Double.parseDouble((bracket < 0 ? expression : expression.substring(0, bracket)).trim());
    }

    public static void validateFullCellConfig(FullCellConfig c) {
        require(c.geometryClassification().equals("representative-microstructure"),
                "geometry.classification must be representative-microstructure");
        require(c.transferenceNumber() > 0 && c.transferenceNumber() < 1,
                "electrolyte.transference.number must be in (0,1)");
        require(c.chargeTransferAlpha() > 0 && c.chargeTransferAlpha() < 1,
                "kinetics.alpha must be in (0,1)");
        require(c.negativeInitialX() >= 0 && c.negativeInitialX() <= 1,
                "negative.initial.x must be in [0,1]");
        require(c.negativeBeta() >= 0, "negative.chemical.expansion.beta must be nonnegative");
        validatePositiveTriplet(c.negativeExchangeCurrentDensitySensitivity(),
                "negative.exchange.current.density.sensitivity.a_m2");
        for (double p : Arrays.asList(c.anodePorosity(), c.cathodePorosity(), c.separatorPorosity())) {
            require(p > 0 && p < 1, "porosities must be in (0,1)");
        }
        for (double t : Arrays.asList(c.anodeTortuosity(), c.cathodeTortuosity(), c.separatorTortuosity())) {
            require(t >= 1, "tortuosities must be >=1");
        }
        require(c.chargeCutoffVoltage() > c.dischargeCutoffVoltage(), "charge cutoff must exceed discharge cutoff");
        require(c.voltageRmseLimit() > 0 && c.provisionalVoltageRmseLimit() >= c.voltageRmseLimit()
                        && c.capacityErrorLimit() > 0 && c.massBalanceErrorLimit() > 0,
                "validation limits must be positive");
        require(!c.snapshotSocFractions().isEmpty()
                        && c.snapshotSocFractions().stream().allMatch(v -> v >= 0 && v <= 1),
                "snapshots.soc.fractions must be in [0,1]");
        require(c.convergenceAverageConcentrationLimit() > 0
                        && c.convergenceConcentrationDeltaLimit() > 0
                        && c.convergenceAverageStressLimit() > 0
                        && c.convergenceStressP95Limit() > 0,
                "convergence limits must be positive");
        require(Arrays.asList("provisional", "literature", "measured").contains(c.parameterStatus().toLowerCase()),
                "full-cell parameter.status must be provisional, literature, or measured");
    }

    public static void validateSimulationConfig(SimulationConfig c) {
        require(c.comsolVersion().equals("6.4"), "This project is currently qualified for COMSOL 6.4");
        require(!c.cRates().isEmpty() && c.cRates().stream().allMatch(v -> v > 0), "all C-rates must be positive");
        require(Arrays.asList("coarse", "normal", "fine", "extra_fine").contains(c.meshLevel()), "invalid mesh.level");
        require(c.meshGrowthRate() > 1, "mesh.growth.rate must be > 1");
        require(c.relativeTolerance() > 0, "solver.relative.tolerance must be positive");
        require(c.concentrationScaleMin() < c.concentrationScaleMax(), "invalid concentration color scale");
    }

    public static void validateRun(String mode, double cRate) {
        require(mode.equals("charge") || mode.equals("discharge"), "mode must be charge or discharge");
        require(Double.isFinite(cRate) && cRate > 0, "C-rate must be finite and positive");
    }

    public static void validateOutputRoot(SimulationConfig c) {
        require(Files.isDirectory(c.outputRoot()), "output root does not exist: " + c.outputRoot());
    }

    public static void validateBuiltModel(Model model) {
        int domains = model.component(ComsolTagUtils.COMPONENT)
                .selection(ComsolTagUtils.PARTICLE_DOMAIN).entities(2).length;
        int axes = model.component(ComsolTagUtils.COMPONENT)
                .selection(ComsolTagUtils.AXIS_BOUNDARY).entities(1).length;
        int surfaces = model.component(ComsolTagUtils.COMPONENT)
                .selection(ComsolTagUtils.SURFACE_BOUNDARY).entities(1).length;
        require(domains == 1, "particle domain count must be 1, got " + domains);
        require(axes > 0, "axis boundary selection is empty");
        require(surfaces > 0, "surface boundary selection is empty");
    }

    public static void validateFullCellModel(Model model) {
        int positive = model.component(ComsolTagUtils.FULL_COMPONENT)
                .selection(ComsolTagUtils.POSITIVE_PARTICLE).entities(3).length;
        int negative = model.component(ComsolTagUtils.FULL_COMPONENT)
                .selection(ComsolTagUtils.NEGATIVE_PARTICLES).entities(3).length;
        int separator = model.component(ComsolTagUtils.FULL_COMPONENT)
                .selection(ComsolTagUtils.SEPARATOR_DOMAIN).entities(3).length;
        int positiveCollector = model.component(ComsolTagUtils.FULL_COMPONENT)
                .selection(ComsolTagUtils.POSITIVE_COLLECTOR).entities(2).length;
        int negativeCollector = model.component(ComsolTagUtils.FULL_COMPONENT)
                .selection(ComsolTagUtils.NEGATIVE_COLLECTOR).entities(2).length;
        require(positive == 1, "full cell must contain exactly one positive particle, got " + positive);
        require(negative == 4, "full cell must contain four hard-carbon particles, got " + negative);
        require(separator > 0, "separator selection is empty");
        require(positiveCollector > 0 && negativeCollector > 0, "current collector boundary selection is empty");
    }

    public static void requireFinite(String name, double value) {
        require(Double.isFinite(value), name + " is NaN or Infinity");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException("Validation failed: " + message);
    }
}
