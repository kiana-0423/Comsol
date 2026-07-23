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
        // VERIFY_WITH_GUI (COMSOL 6.4): feature identifiers and property keys below are
        // intentionally centralized. Compare with a minimal GUI model saved as Java.
        model.component(ComsolTagUtils.FULL_COMPONENT).physics().create(ComsolTagUtils.FULL_BATTERY,
                ComsolTagUtils.PHYSICS_LITHIUM_ION_BATTERY, ComsolTagUtils.FULL_GEOMETRY);
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_BATTERY).selection().named(ComsolTagUtils.ELECTROLYTE_DOMAINS);
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_BATTERY)
                .feature("socicd1").feature("neges1").selection().named(ComsolTagUtils.ANODE_MATRIX);
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_BATTERY)
                .feature("socicd1").feature("poses1").selection().named(ComsolTagUtils.CATHODE_MATRIX);
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_BATTERY)
                .feature("socicd1").feature("negebs1").selection().named(ComsolTagUtils.NEGATIVE_COLLECTOR);
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_BATTERY)
                .feature("socicd1").feature("posebs1").selection().named(ComsolTagUtils.POSITIVE_COLLECTOR);
        // The 3D interface has no root-node cross-sectional-area setting. A_cell
        // remains a model parameter for applied-current and capacity conversion.
        // Electrolyte initial values belong to the default Initial Values feature.
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_BATTERY)
                .feature("init1").set("cl", "cl0");

        batteryCreate(model, "pcb_neg", ComsolTagUtils.FEATURE_POROUS_CONDUCTIVE_BINDER, 3,
                ComsolTagUtils.ANODE_MATRIX);
        batterySet(model, "pcb_neg", "sigma_mat", "userdef");
        batterySet(model, "pcb_neg", "sigma", "sigma_binder");
        batterySet(model, "pcb_neg", "Tref_mat", "userdef");
        batterySet(model, "pcb_neg", "alpha_mat", "userdef");
        batterySet(model, "pcb_neg", "rho0_mat", "userdef");
        batterySet(model, "pcb_neg", "AddVolumeChangeToElectrodeVolumeFraction", "0");
        batterySet(model, "pcb_neg", "SubtractVolumeChangeFromElectrolyteVolumeFraction", "0");
        batterySet(model, "pcb_neg", "epsl", "eps_an");
        batterySet(model, "pcb_neg", "taul", "tau_an");
        setElectrolyteProperties(model, "pcb_neg");

        // sep1 is a mandatory default feature. Its locked selection is the
        // remainder after the SOC node's negative/positive electrode domains.
        batterySet(model, "sep1", "epsl", "eps_sep");
        batterySet(model, "sep1", "taul", "tau_sep");
        setElectrolyteProperties(model, "sep1");

        batteryCreate(model, "pcb_pos", ComsolTagUtils.FEATURE_POROUS_CONDUCTIVE_BINDER, 3,
                ComsolTagUtils.CATHODE_MATRIX);
        batterySet(model, "pcb_pos", "sigma_mat", "userdef");
        batterySet(model, "pcb_pos", "sigma", "sigma_binder");
        batterySet(model, "pcb_pos", "Tref_mat", "userdef");
        batterySet(model, "pcb_pos", "alpha_mat", "userdef");
        batterySet(model, "pcb_pos", "rho0_mat", "userdef");
        batterySet(model, "pcb_pos", "AddVolumeChangeToElectrodeVolumeFraction", "0");
        batterySet(model, "pcb_pos", "SubtractVolumeChangeFromElectrolyteVolumeFraction", "0");
        batterySet(model, "pcb_pos", "epsl", "eps_ca");
        batterySet(model, "pcb_pos", "taul", "tau_ca");
        setElectrolyteProperties(model, "pcb_pos");

        batteryCreate(model, "ies_neg", ComsolTagUtils.FEATURE_INTERNAL_ELECTRODE_SURFACE, 2,
                ComsolTagUtils.NEGATIVE_SURFACES);
        batterySet(model, "ies_neg", "SolveForDissolvingDepositingConcentrationVariable", "0");
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_BATTERY)
                .feature("ies_neg").feature("er1").set("Eeq_mat", "userdef");
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_BATTERY)
                .feature("ies_neg").feature("er1").set("ElectrodeKinetics", "LithiumInsertion");
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_BATTERY)
                .feature("ies_neg").feature("er1").set("minput_concentration", "cNeg");
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_BATTERY)
                .feature("ies_neg").feature("er1").set("cEeqref_mat", "userdef");
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_BATTERY)
                .feature("ies_neg").feature("er1").set("cEeqref", "csmax_neg");
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_BATTERY)
                .feature("ies_neg").feature("er1").set("Eeq", "Eeq_neg(xNegBattery)");
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_BATTERY)
                .feature("ies_neg").feature("er1").set("dEeqdT_mat", "userdef");
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_BATTERY)
                .feature("ies_neg").feature("er1").set("i0", "i0_neg(xNegBattery)*negativeKineticsScale");

        batteryCreate(model, "ies_pos", ComsolTagUtils.FEATURE_INTERNAL_ELECTRODE_SURFACE, 2,
                ComsolTagUtils.POSITIVE_SURFACE);
        batterySet(model, "ies_pos", "SolveForDissolvingDepositingConcentrationVariable", "0");
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_BATTERY)
                .feature("ies_pos").feature("er1").set("Eeq_mat", "userdef");
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_BATTERY)
                .feature("ies_pos").feature("er1").set("ElectrodeKinetics", "LithiumInsertion");
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_BATTERY)
                .feature("ies_pos").feature("er1").set("minput_concentration", "cPos");
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_BATTERY)
                .feature("ies_pos").feature("er1").set("cEeqref_mat", "userdef");
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_BATTERY)
                .feature("ies_pos").feature("er1").set("cEeqref", "csmax_pos");
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_BATTERY)
                .feature("ies_pos").feature("er1").set("Eeq", "Eeq_pos(xPosBattery)");
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_BATTERY)
                .feature("ies_pos").feature("er1").set("dEeqdT_mat", "userdef");
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_BATTERY)
                .feature("ies_pos").feature("er1").set("i0", "i0_pos");

        batteryCreate(model, "ground_neg", ComsolTagUtils.FEATURE_ELECTRIC_POTENTIAL, 2,
                ComsolTagUtils.NEGATIVE_COLLECTOR);
        batteryCreate(model, "current_pos", ComsolTagUtils.FEATURE_ELECTRODE_CURRENT, 2,
                ComsolTagUtils.POSITIVE_COLLECTOR);
        // I_app is the current of one micrometre-scale representative cell
        // (about 1e-12 A at 0.1C). Enforcing that tiny number as TotalCurrent
        // badly scales the global boundary-potential constraint. COMSOL 6.4's
        // equivalent AverageCurrentDensity form uses Ias and avoids that
        // artificial singularity without changing the applied current.
        batterySet(model, "current_pos", "ElectronicCurrentType", "AverageCurrentDensity");
        batterySet(model, "current_pos", "Ias", "runDirection*j_app");
    }

    private void buildParticleDiffusion(Model model, String tag, String field, String initial,
                                        String diffusivity, String flux, String domains, String surfaces) {
        model.component(ComsolTagUtils.FULL_COMPONENT).physics().create(
                tag, ComsolTagUtils.PHYSICS_COEFFICIENT_PDE, ComsolTagUtils.FULL_GEOMETRY);
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(tag).selection().named(domains);
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(tag).field("dimensionless").field(field);
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(tag).field("dimensionless").component(new String[]{field});
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(tag).prop("Units").set("DependentVariableQuantity", "concentration");
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(tag).feature("cfeq1").set("da", "1");
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(tag).feature("cfeq1").set("c", diffusivity);
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(tag).feature("cfeq1").set("a", "0");
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(tag).feature("cfeq1").set("f", "0");
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(tag).feature("init1").set(field, initial);
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(tag).create("surface_flux", ComsolTagUtils.FEATURE_PDE_FLUX, 2);
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(tag).feature("surface_flux").selection().named(surfaces);
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(tag).feature("surface_flux").set("g", flux);
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(tag).feature("surface_flux").set("q", "0");
    }

    private void buildMechanics(Model model) {
        model.component(ComsolTagUtils.FULL_COMPONENT).physics().create(
                ComsolTagUtils.FULL_SOLID, ComsolTagUtils.PHYSICS_SOLID, ComsolTagUtils.FULL_GEOMETRY);
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_SOLID).selection().named(ComsolTagUtils.POSITIVE_PARTICLE);
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_SOLID).feature("lemm1").set("E_mat", "userdef");
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_SOLID).feature("lemm1").set("E", "E_pos");
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_SOLID).feature("lemm1").set("nu_mat", "userdef");
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_SOLID).feature("lemm1").set("nu", "nu_pos");
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_SOLID).feature("lemm1").set("rho_mat", "userdef");
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_SOLID).feature("lemm1")
                .create("chemical_strain", ComsolTagUtils.FEATURE_EXTERNAL_STRAIN, 3);
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_SOLID).feature("lemm1")
                .feature("chemical_strain").set("StrainInput", "StrainTensor");
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_SOLID).feature("lemm1")
                .feature("chemical_strain").set("eext_src", "userdef");
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_SOLID).feature("lemm1")
                .feature("chemical_strain").set("eext",
                new String[]{"epsilonChemPos", "0", "0",
                        "0", "epsilonChemPos", "0",
                        "0", "0", "epsilonChemPos"});
        // Free-particle mechanics: suppress only rigid-body modes, without constraining expansion.
        // VERIFY_WITH_GUI (6.4): confirm RigidMotionSuppression feature dimension/property.
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_SOLID)
                .create("rigid_motion_suppression", ComsolTagUtils.FEATURE_RIGID_MOTION_SUPPRESSION, 3);
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_SOLID)
                .feature("rigid_motion_suppression").selection().named(ComsolTagUtils.POSITIVE_PARTICLE);
    }

    private void batteryCreate(Model model, String tag, String type, int dimension, String selection) {
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_BATTERY)
                .create(tag, type, dimension);
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_BATTERY)
                .feature(tag).selection().named(selection);
    }

    private void batterySet(Model model, String feature, String key, String value) {
        model.component(ComsolTagUtils.FULL_COMPONENT).physics(ComsolTagUtils.FULL_BATTERY)
                .feature(feature).set(key, value);
    }

    private void setElectrolyteProperties(Model model, String feature) {
        batterySet(model, feature, "sigmal_mat", "userdef");
        batterySet(model, feature, "sigmal", "sigma_l_fun(cl/1[kmol/m^3])");
        batterySet(model, feature, "Dl_mat", "userdef");
        batterySet(model, feature, "Dl", "Dl");
        batterySet(model, feature, "transpNum_mat", "userdef");
        batterySet(model, feature, "transpNum", "tplus");
        batterySet(model, feature, "fcl_mat", "userdef");
        batterySet(model, feature, "fcl", "1");
    }
}
