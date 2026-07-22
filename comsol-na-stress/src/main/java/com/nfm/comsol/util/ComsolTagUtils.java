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
    public static final String FEATURE_RIGID_MOTION_SUPPRESSION = "RigidMotionSuppression";

    // 3D heterogeneous representative-cell tags. Keep feature type strings here because
    // Battery Module identifiers are version-sensitive and must be checked against 6.4 GUI export.
    public static final String FULL_COMPONENT = "comp_full";
    public static final String FULL_GEOMETRY = "geom_full";
    public static final String FULL_MESH = "mesh_full";
    public static final String ANODE_MATRIX = "anode_matrix_domain";
    public static final String SEPARATOR_DOMAIN = "separator_domain";
    public static final String CATHODE_MATRIX = "cathode_matrix_domain";
    public static final String NEGATIVE_PARTICLES = "negative_particle_domains";
    public static final String POSITIVE_PARTICLE = "positive_particle_domain";
    public static final String NEGATIVE_SURFACES = "negative_particle_surfaces";
    public static final String POSITIVE_SURFACE = "positive_particle_surface";
    public static final String NEGATIVE_COLLECTOR = "negative_collector_boundary";
    public static final String POSITIVE_COLLECTOR = "positive_collector_boundary";
    public static final String LATERAL_BOUNDARIES = "lateral_boundaries";
    public static final String BINDER_DOMAINS = "binder_domains";
    public static final String FULL_CELL_DOMAINS = "full_cell_domains";
    public static final String ELECTROLYTE_DOMAINS = "electrolyte_domains";
    public static final String FULL_BATTERY = "liion";
    public static final String NEGATIVE_DIFFUSION = "cNa_negative";
    public static final String POSITIVE_DIFFUSION = "cNa_positive";
    public static final String FULL_SOLID = "solid_full";
    public static final String FULL_STUDY_INIT = "study_full_init";
    public static final String FULL_STUDY_CHARGE = "study_full_charge";
    public static final String FULL_STUDY_DISCHARGE = "study_full_discharge";
    public static final String FULL_DATASET_POSITIVE_CUTLINE = "positive_radial_cutline";

    public static final String PHYSICS_LITHIUM_ION_BATTERY = "LithiumIonBattery";
    public static final String FEATURE_SEPARATOR = "Separator";
    public static final String FEATURE_POROUS_CONDUCTIVE_BINDER = "PorousConductiveBinder";
    public static final String FEATURE_INTERNAL_ELECTRODE_SURFACE = "InternalElectrodeSurface";
    public static final String FEATURE_ELECTRODE_CURRENT = "ElectrodeCurrent";
    public static final String FEATURE_ELECTRIC_POTENTIAL = "ElectricPotential";
    public static final String POSITIVE_LOCAL_CURRENT = "liion.ies_pos.er1.iloc";
    public static final String NEGATIVE_LOCAL_CURRENT = "liion.ies_neg.er1.iloc";
}
