package com.nfm.comsol.model;

import com.comsol.model.Model;
import com.nfm.comsol.config.SimulationConfig;
import com.nfm.comsol.util.ComsolTagUtils;

public final class MeshBuilder {
    public void build(Model model, SimulationConfig config, boolean smokeTest) {
        model.component(ComsolTagUtils.COMPONENT).mesh().create(ComsolTagUtils.MESH);
        model.component(ComsolTagUtils.COMPONENT).mesh(ComsolTagUtils.MESH).create("size_particle", "Size");
        model.component(ComsolTagUtils.COMPONENT).mesh(ComsolTagUtils.MESH).feature("size_particle").selection().geom(ComsolTagUtils.GEOMETRY, 2);
        model.component(ComsolTagUtils.COMPONENT).mesh(ComsolTagUtils.MESH).feature("size_particle").selection().named(ComsolTagUtils.PARTICLE_DOMAIN);
        model.component(ComsolTagUtils.COMPONENT).mesh(ComsolTagUtils.MESH).feature("size_particle").set("custom", "on");
        model.component(ComsolTagUtils.COMPONENT).mesh(ComsolTagUtils.MESH).feature("size_particle").set("hmax", smokeTest ? config.smokeMeshSize() : hmax(config));
        model.component(ComsolTagUtils.COMPONENT).mesh(ComsolTagUtils.MESH).feature("size_particle").set("hmin", config.meshMinSize());
        model.component(ComsolTagUtils.COMPONENT).mesh(ComsolTagUtils.MESH).feature("size_particle").set("hgrad", Double.toString(config.meshGrowthRate()));

        model.component(ComsolTagUtils.COMPONENT).mesh(ComsolTagUtils.MESH).create("surface_size", "Size");
        model.component(ComsolTagUtils.COMPONENT).mesh(ComsolTagUtils.MESH).feature("surface_size").selection().geom(ComsolTagUtils.GEOMETRY, 1);
        model.component(ComsolTagUtils.COMPONENT).mesh(ComsolTagUtils.MESH).feature("surface_size").selection().named(ComsolTagUtils.SURFACE_BOUNDARY);
        model.component(ComsolTagUtils.COMPONENT).mesh(ComsolTagUtils.MESH).feature("surface_size").set("custom", "on");
        model.component(ComsolTagUtils.COMPONENT).mesh(ComsolTagUtils.MESH).feature("surface_size").set("hmax", smokeTest ? config.smokeMeshSize() : "0.5*(" + hmax(config) + ")");

        model.component(ComsolTagUtils.COMPONENT).mesh(ComsolTagUtils.MESH).create("free_triangles", "FreeTri");
        model.component(ComsolTagUtils.COMPONENT).mesh(ComsolTagUtils.MESH).feature("free_triangles").selection().geom(ComsolTagUtils.GEOMETRY, 2);
        model.component(ComsolTagUtils.COMPONENT).mesh(ComsolTagUtils.MESH).feature("free_triangles").selection().named(ComsolTagUtils.PARTICLE_DOMAIN);
        model.component(ComsolTagUtils.COMPONENT).mesh(ComsolTagUtils.MESH).run();
    }

    private String hmax(SimulationConfig c) {
        if ("coarse".equals(c.meshLevel())) return "2*(" + c.meshMaxSize() + ")";
        if ("fine".equals(c.meshLevel())) return "0.67*(" + c.meshMaxSize() + ")";
        if ("extra_fine".equals(c.meshLevel())) return "0.5*(" + c.meshMaxSize() + ")";
        return c.meshMaxSize();
    }
}
