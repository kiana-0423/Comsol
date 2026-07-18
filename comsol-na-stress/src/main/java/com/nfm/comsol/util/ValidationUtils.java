package com.nfm.comsol.util;

import com.comsol.model.Model;
import com.nfm.comsol.config.MaterialConfig;
import com.nfm.comsol.config.SimulationConfig;

import java.nio.file.Files;
import java.util.List;

public final class ValidationUtils {
    private ValidationUtils() {}

    public static void validateMaterialConfig(MaterialConfig c) {
        require(c.initialX() >= 0 && c.initialX() <= 1, "initial.x must be in [0,1]");
        require(c.finalChargeX() >= 0 && c.finalChargeX() < c.initialX(), "final.x.charge must be >=0 and below initial.x");
        require(c.poissonRatio() > -1 && c.poissonRatio() < 0.5, "poisson.ratio must be between -1 and 0.5");
        require(c.beta() >= 0, "chemical.expansion.beta must be nonnegative");
        require(List.of("constant", "interpolation").contains(c.diffusionMode()), "invalid diffusion.mode");
        require(List.of("linear", "interpolation", "phase_transition").contains(c.strainMode()), "invalid strain.mode");
        require(c.phaseSmoothingWidth() > 0, "phase.smoothing.width must be positive");
        require(c.gradientExponent() > 0, "gradient.exponent must be positive");
        require(c.parameterStatus().equalsIgnoreCase("provisional"), "stage-1 parameters must remain marked provisional");
    }

    public static void validateSimulationConfig(SimulationConfig c) {
        require(c.comsolVersion().equals("6.4"), "This project is currently qualified for COMSOL 6.4");
        require(!c.cRates().isEmpty() && c.cRates().stream().allMatch(v -> v > 0), "all C-rates must be positive");
        require(List.of("coarse", "normal", "fine", "extra_fine").contains(c.meshLevel()), "invalid mesh.level");
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

    public static void requireFinite(String name, double value) {
        require(Double.isFinite(value), name + " is NaN or Infinity");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException("Validation failed: " + message);
    }
}
