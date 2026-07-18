package com.nfm.comsol.model;

import com.comsol.model.Model;
import com.nfm.comsol.util.ComsolTagUtils;

/** Coefficient-form PDE implementation of dc/dt = div(D grad(c)). */
public final class DiffusionPhysicsBuilder {
    public void build(Model model) {
        var component = model.component(ComsolTagUtils.COMPONENT);
        component.physics().create(ComsolTagUtils.DIFFUSION,
                ComsolTagUtils.PHYSICS_COEFFICIENT_PDE, ComsolTagUtils.GEOMETRY);
        var physics = component.physics(ComsolTagUtils.DIFFUSION);
        physics.selection().named(ComsolTagUtils.PARTICLE_DOMAIN);
        physics.field("dimensionless").field("cNa");
        physics.field("dimensionless").component(new String[]{"cNa"});
        physics.prop("Units").set("DependentVariableQuantity", "concentration");

        physics.feature("cfeq1").set("da", "1");
        physics.feature("cfeq1").set("c", "DsEffective");
        physics.feature("cfeq1").set("a", "0");
        physics.feature("cfeq1").set("f", "0");
        physics.feature("init1").set("cNa", "cs0");

        // PDE natural boundary g equals D*normal_gradient(c); negative g removes Na.
        physics.create("surface_flux", ComsolTagUtils.FEATURE_PDE_FLUX, 1);
        physics.feature("surface_flux").selection().named(ComsolTagUtils.SURFACE_BOUNDARY);
        physics.feature("surface_flux").set("g", "signedNaFlux");
        physics.feature("surface_flux").set("q", "0");

        physics.create("axis_no_flux", ComsolTagUtils.FEATURE_PDE_ZERO_FLUX, 1);
        physics.feature("axis_no_flux").selection().named(ComsolTagUtils.AXIS_BOUNDARY);
    }
}
