package com.nfm.comsol.model;

import com.comsol.model.Model;
import com.nfm.comsol.util.ComsolTagUtils;

public final class SolidMechanicsBuilder {
    public void build(Model model) {
        model.component(ComsolTagUtils.COMPONENT).physics().create(
                ComsolTagUtils.SOLID, ComsolTagUtils.PHYSICS_SOLID, ComsolTagUtils.GEOMETRY);
        model.component(ComsolTagUtils.COMPONENT).physics(ComsolTagUtils.SOLID)
                .selection().named(ComsolTagUtils.PARTICLE_DOMAIN);
        model.component(ComsolTagUtils.COMPONENT).physics(ComsolTagUtils.SOLID)
                .feature("lemm1").set("E", "E_particle");
        model.component(ComsolTagUtils.COMPONENT).physics(ComsolTagUtils.SOLID)
                .feature("lemm1").set("nu", "nu_particle");
        model.component(ComsolTagUtils.COMPONENT).physics(ComsolTagUtils.SOLID)
                .feature("lemm1").set("rho", "rho_p");

        // Axisymmetric formulation supplies u_r=0 on r=0. Fixing only the origin removes z translation.
        model.component(ComsolTagUtils.COMPONENT).physics(ComsolTagUtils.SOLID)
                .create("origin_constraint", "Fixed", 0);
        model.component(ComsolTagUtils.COMPONENT).physics(ComsolTagUtils.SOLID)
                .feature("origin_constraint").selection().named(ComsolTagUtils.FIX_POINT);
        // The curved outer surface has no applied mechanical feature and is therefore traction-free.
    }
}
