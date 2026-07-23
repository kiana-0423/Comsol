package com.nfm.comsol.model;

import com.comsol.model.Model;
import com.nfm.comsol.util.ComsolTagUtils;

/** Coefficient-form PDE implementation of dc/dt = div(D grad(c)). */
public final class DiffusionPhysicsBuilder {
    public void build(Model model) {
        model.component(ComsolTagUtils.COMPONENT).physics().create(ComsolTagUtils.DIFFUSION,
                ComsolTagUtils.PHYSICS_COEFFICIENT_PDE, ComsolTagUtils.GEOMETRY);
        model.component(ComsolTagUtils.COMPONENT).physics(ComsolTagUtils.DIFFUSION)
                .selection().named(ComsolTagUtils.PARTICLE_DOMAIN);
        model.component(ComsolTagUtils.COMPONENT).physics(ComsolTagUtils.DIFFUSION)
                .field("dimensionless").field("cNa");
        model.component(ComsolTagUtils.COMPONENT).physics(ComsolTagUtils.DIFFUSION)
                .field("dimensionless").component(new String[]{"cNa"});
        model.component(ComsolTagUtils.COMPONENT).physics(ComsolTagUtils.DIFFUSION)
                .prop("Units").set("DependentVariableQuantity", "concentration");

        model.component(ComsolTagUtils.COMPONENT).physics(ComsolTagUtils.DIFFUSION).feature("cfeq1").set("da", "1");
        model.component(ComsolTagUtils.COMPONENT).physics(ComsolTagUtils.DIFFUSION).feature("cfeq1").set("c", "DsEffective");
        model.component(ComsolTagUtils.COMPONENT).physics(ComsolTagUtils.DIFFUSION).feature("cfeq1").set("a", "0");
        model.component(ComsolTagUtils.COMPONENT).physics(ComsolTagUtils.DIFFUSION).feature("cfeq1").set("f", "0");
        model.component(ComsolTagUtils.COMPONENT).physics(ComsolTagUtils.DIFFUSION).feature("init1").set("cNa", "cs0");

        // PDE natural boundary g equals D*normal_gradient(c); negative g removes Na.
        model.component(ComsolTagUtils.COMPONENT).physics(ComsolTagUtils.DIFFUSION)
                .create("surface_flux", ComsolTagUtils.FEATURE_PDE_FLUX, 1);
        model.component(ComsolTagUtils.COMPONENT).physics(ComsolTagUtils.DIFFUSION)
                .feature("surface_flux").selection().named(ComsolTagUtils.SURFACE_BOUNDARY);
        model.component(ComsolTagUtils.COMPONENT).physics(ComsolTagUtils.DIFFUSION)
                .feature("surface_flux").set("g", "signedNaFlux");
        model.component(ComsolTagUtils.COMPONENT).physics(ComsolTagUtils.DIFFUSION)
                .feature("surface_flux").set("q", "0");

        model.component(ComsolTagUtils.COMPONENT).physics(ComsolTagUtils.DIFFUSION)
                .create("axis_no_flux", ComsolTagUtils.FEATURE_PDE_ZERO_FLUX, 1);
        model.component(ComsolTagUtils.COMPONENT).physics(ComsolTagUtils.DIFFUSION)
                .feature("axis_no_flux").selection().named(ComsolTagUtils.AXIS_BOUNDARY);
    }
}
