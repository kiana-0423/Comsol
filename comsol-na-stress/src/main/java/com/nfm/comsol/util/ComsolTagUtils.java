package com.nfm.comsol.util;

/** Central registry for every COMSOL model tag and version-sensitive API identifier. */
public final class ComsolTagUtils {
    private ComsolTagUtils() {}
    public static final String MODEL = "Model";
    public static final String COMPONENT = "comp1";
    public static final String GEOMETRY = "geom1";
    public static final String MESH = "mesh1";
    public static final String PARTICLE_DOMAIN = "particle_domain";
    public static final String AXIS_BOUNDARY = "axis_boundary";
    public static final String SURFACE_BOUNDARY = "surface_boundary";
    public static final String FIX_POINT = "fix_point_or_constraint";
    public static final String DIFFUSION = "cNaPhysics";
    public static final String SOLID = "solid";
    public static final String STUDY_CHARGE = "study_charge";
    public static final String STUDY_DISCHARGE = "study_discharge";
    public static final String DATASET_CUTLINE = "radial_cutline";
    public static final String SOLUTION_CHARGE = "sol_charge";
    public static final String SOLUTION_DISCHARGE = "sol_discharge";

    // VERIFY_WITH_GUI (COMSOL 6.4): compare these with File > Save As > Model File for Java.
    public static final String PHYSICS_COEFFICIENT_PDE = "CoefficientFormPDE";
    public static final String PHYSICS_SOLID = "SolidMechanics";
    public static final String FEATURE_PDE_FLUX = "FluxBoundary";
    public static final String FEATURE_PDE_ZERO_FLUX = "ZeroFluxBoundary";
    public static final String FEATURE_EXTERNAL_STRAIN = "ExternalStrain";
}
