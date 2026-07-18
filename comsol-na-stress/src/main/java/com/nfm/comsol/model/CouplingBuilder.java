package com.nfm.comsol.model;

import com.comsol.model.Model;
import com.nfm.comsol.util.ComsolTagUtils;

/** Injects equal rr, phi-phi and zz chemical eigenstrain into linear elasticity. */
public final class CouplingBuilder {
    public void build(Model model) {
        var linear = model.component(ComsolTagUtils.COMPONENT).physics(ComsolTagUtils.SOLID).feature("lemm1");
        // VERIFY_WITH_GUI (COMSOL 6.4): ExternalStrain/e0Voigt property names are version sensitive.
        // A failure is intentional and visible; there is no silent fallback to fake thermal physics.
        linear.create("chemical_strain", ComsolTagUtils.FEATURE_EXTERNAL_STRAIN, 2);
        linear.feature("chemical_strain").selection().named(ComsolTagUtils.PARTICLE_DOMAIN);
        linear.feature("chemical_strain").set("e0Voigt",
                new String[]{"epsilonChem", "epsilonChem", "epsilonChem", "0"});
    }
}
