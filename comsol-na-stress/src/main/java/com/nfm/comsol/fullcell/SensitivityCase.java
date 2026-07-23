package com.nfm.comsol.fullcell;

/** Independent scaling factors for one full-cell sensitivity or attribution case. */
public final class SensitivityCase {
    private final String name;
    private final double chargeDiffusionScale, dischargeDiffusionScale, strainScale;
    private final double modulusScale, poissonScale, radiusScale;
    private final double positiveKineticsScale, negativeKineticsScale;

    public SensitivityCase(String name, double chargeDiffusionScale,
                           double dischargeDiffusionScale, double strainScale,
                           double modulusScale, double poissonScale, double radiusScale,
                           double positiveKineticsScale, double negativeKineticsScale) {
        this.name = name;
        this.chargeDiffusionScale = chargeDiffusionScale;
        this.dischargeDiffusionScale = dischargeDiffusionScale;
        this.strainScale = strainScale;
        this.modulusScale = modulusScale;
        this.poissonScale = poissonScale;
        this.radiusScale = radiusScale;
        this.positiveKineticsScale = positiveKineticsScale;
        this.negativeKineticsScale = negativeKineticsScale;
    }

    public String name() { return name; }
    public double chargeDiffusionScale() { return chargeDiffusionScale; }
    public double dischargeDiffusionScale() { return dischargeDiffusionScale; }
    public double strainScale() { return strainScale; }
    public double modulusScale() { return modulusScale; }
    public double poissonScale() { return poissonScale; }
    public double radiusScale() { return radiusScale; }
    public double positiveKineticsScale() { return positiveKineticsScale; }
    public double negativeKineticsScale() { return negativeKineticsScale; }

    public static SensitivityCase baseline() {
        return new SensitivityCase("baseline", 1, 1, 1, 1, 1, 1, 1, 1);
    }
}
