package com.nfm.comsol.fullcell;

/** Independent scaling factors for one full-cell sensitivity or attribution case. */
public record SensitivityCase(
        String name,
        double chargeDiffusionScale,
        double dischargeDiffusionScale,
        double strainScale,
        double modulusScale,
        double poissonScale,
        double radiusScale,
        double positiveKineticsScale,
        double negativeKineticsScale) {

    public static SensitivityCase baseline() {
        return new SensitivityCase("baseline", 1, 1, 1, 1, 1, 1, 1, 1);
    }
}
