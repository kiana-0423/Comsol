package com.nfm.comsol.model;

import com.comsol.model.Model;
import com.nfm.comsol.util.ComsolTagUtils;

public final class SolidMechanicsBuilder {
    public void build(Model model) {
        var component = model.component(ComsolTagUtils.COMPONENT);
        component.physics().create(ComsolTagUtils.SOLID, ComsolTagUtils.PHYSICS_SOLID, ComsolTagUtils.GEOMETRY);
        var solid = component.physics(ComsolTagUtils.SOLID);
        solid.selection().named(ComsolTagUtils.PARTICLE_DOMAIN);
        solid.feature("lemm1").set("E", "E_particle");
        solid.feature("lemm1").set("nu", "nu_particle");
        solid.feature("lemm1").set("rho", "rho_p");

        // Axisymmetric formulation supplies u_r=0 on r=0. Fixing only the origin removes z translation.
        solid.create("origin_constraint", "Fixed", 0);
        solid.feature("origin_constraint").selection().named(ComsolTagUtils.FIX_POINT);
        // The curved outer surface has no applied mechanical feature and is therefore traction-free.
    }
}
