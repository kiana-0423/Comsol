package com.nfm.comsol.fullcell;

import com.comsol.model.Model;
import com.nfm.comsol.util.ComsolTagUtils;

/** Battery Module, explicit solid diffusion and concentration-strain mechanics. */
public final class FullCellPhysicsBuilder {
    public void build(Model model) {
        buildBattery(model);
        buildParticleDiffusion(model, ComsolTagUtils.POSITIVE_DIFFUSION, "cPos", "cs0_pos",
                "DsPos", "positiveNaFlux", ComsolTagUtils.POSITIVE_PARTICLE, ComsolTagUtils.POSITIVE_SURFACE);
        buildParticleDiffusion(model, ComsolTagUtils.NEGATIVE_DIFFUSION, "cNeg", "cs0_neg",
                "DsNegEffective", "negativeNaFlux", ComsolTagUtils.NEGATIVE_PARTICLES, ComsolTagUtils.NEGATIVE_SURFACES);
        buildMechanics(model);
    }

    private void buildBattery(Model model) {
        var component = model.component(ComsolTagUtils.FULL_COMPONENT);
        // VERIFY_WITH_GUI (COMSOL 6.4): feature identifiers and property keys below are
        // intentionally centralized. Compare with a minimal GUI model saved as Java.
        component.physics().create(ComsolTagUtils.FULL_BATTERY,
                ComsolTagUtils.PHYSICS_LITHIUM_ION_BATTERY, ComsolTagUtils.FULL_GEOMETRY);
        var battery = component.physics(ComsolTagUtils.FULL_BATTERY);
        battery.selection().named(ComsolTagUtils.ELECTROLYTE_DOMAINS);
        battery.set("Ac", "A_cell");
        battery.set("cl0", "cl0");
        battery.set("tplus", "tplus");
        battery.set("Dl", "Dl");
        battery.set("sigmal", "sigma_l_fun(cl/1[kmol/m^3])");

        battery.create("pcb_neg", ComsolTagUtils.FEATURE_POROUS_CONDUCTIVE_BINDER, 3);
        battery.feature("pcb_neg").selection().named(ComsolTagUtils.ANODE_MATRIX);
        battery.feature("pcb_neg").set("sigmas", "sigma_binder");
        battery.feature("pcb_neg").set("epsilonl", "eps_an");
        battery.feature("pcb_neg").set("taul", "tau_an");

        battery.create("separator", ComsolTagUtils.FEATURE_SEPARATOR, 3);
        battery.feature("separator").selection().named(ComsolTagUtils.SEPARATOR_DOMAIN);
        battery.feature("separator").set("epsilonl", "eps_sep");
        battery.feature("separator").set("taul", "tau_sep");

        battery.create("pcb_pos", ComsolTagUtils.FEATURE_POROUS_CONDUCTIVE_BINDER, 3);
        battery.feature("pcb_pos").selection().named(ComsolTagUtils.CATHODE_MATRIX);
        battery.feature("pcb_pos").set("sigmas", "sigma_binder");
        battery.feature("pcb_pos").set("epsilonl", "eps_ca");
        battery.feature("pcb_pos").set("taul", "tau_ca");

        battery.create("ies_neg", ComsolTagUtils.FEATURE_INTERNAL_ELECTRODE_SURFACE, 2);
        battery.feature("ies_neg").selection().named(ComsolTagUtils.NEGATIVE_SURFACES);
        battery.feature("ies_neg").feature("er1").set("Eeq", "Eeq_neg(xNegBattery)");
        battery.feature("ies_neg").feature("er1").set("i0", "i0_neg(xNegBattery)*kineticsScale");

        battery.create("ies_pos", ComsolTagUtils.FEATURE_INTERNAL_ELECTRODE_SURFACE, 2);
        battery.feature("ies_pos").selection().named(ComsolTagUtils.POSITIVE_SURFACE);
        battery.feature("ies_pos").feature("er1").set("Eeq", "Eeq_pos(xPosBattery)");
        battery.feature("ies_pos").feature("er1").set("i0", "i0_pos");

        battery.create("ground_neg", ComsolTagUtils.FEATURE_ELECTRIC_POTENTIAL, 2);
        battery.feature("ground_neg").selection().named(ComsolTagUtils.NEGATIVE_COLLECTOR);
        battery.feature("ground_neg").set("phis0", "0[V]");
        battery.create("current_pos", ComsolTagUtils.FEATURE_ELECTRODE_CURRENT, 2);
        battery.feature("current_pos").selection().named(ComsolTagUtils.POSITIVE_COLLECTOR);
        battery.feature("current_pos").set("IsTotal", "runDirection*I_app");
    }

    private void buildParticleDiffusion(Model model, String tag, String field, String initial,
                                        String diffusivity, String flux, String domains, String surfaces) {
        var component = model.component(ComsolTagUtils.FULL_COMPONENT);
        component.physics().create(tag, ComsolTagUtils.PHYSICS_COEFFICIENT_PDE, ComsolTagUtils.FULL_GEOMETRY);
        var physics = component.physics(tag);
        physics.selection().named(domains);
        physics.field("dimensionless").field(field);
        physics.field("dimensionless").component(new String[]{field});
        physics.prop("Units").set("DependentVariableQuantity", "concentration");
        physics.feature("cfeq1").set("da", "1");
        physics.feature("cfeq1").set("c", diffusivity);
        physics.feature("cfeq1").set("a", "0");
        physics.feature("cfeq1").set("f", "0");
        physics.feature("init1").set(field, initial);
        physics.create("surface_flux", ComsolTagUtils.FEATURE_PDE_FLUX, 2);
        physics.feature("surface_flux").selection().named(surfaces);
        physics.feature("surface_flux").set("g", flux);
        physics.feature("surface_flux").set("q", "0");
    }

    private void buildMechanics(Model model) {
        var component = model.component(ComsolTagUtils.FULL_COMPONENT);
        component.physics().create(ComsolTagUtils.FULL_SOLID, ComsolTagUtils.PHYSICS_SOLID, ComsolTagUtils.FULL_GEOMETRY);
        var solid = component.physics(ComsolTagUtils.FULL_SOLID);
        solid.selection().named(ComsolTagUtils.POSITIVE_PARTICLE);
        solid.feature("lemm1").selection().named(ComsolTagUtils.POSITIVE_PARTICLE);
        solid.feature("lemm1").set("E", "E_pos");
        solid.feature("lemm1").set("nu", "nu_pos");
        solid.feature("lemm1").create("chemical_strain", ComsolTagUtils.FEATURE_EXTERNAL_STRAIN, 3);
        solid.feature("lemm1").feature("chemical_strain").selection().named(ComsolTagUtils.POSITIVE_PARTICLE);
        solid.feature("lemm1").feature("chemical_strain").set("e0Voigt",
                new String[]{"epsilonChemPos", "epsilonChemPos", "epsilonChemPos", "0", "0", "0"});
        // Free-particle mechanics: suppress only rigid-body modes, without constraining expansion.
        // VERIFY_WITH_GUI (6.4): confirm RigidMotionSuppression feature dimension/property.
        solid.create("rigid_motion_suppression", ComsolTagUtils.FEATURE_RIGID_MOTION_SUPPRESSION, 3);
        solid.feature("rigid_motion_suppression").selection().named(ComsolTagUtils.POSITIVE_PARTICLE);
    }
}
