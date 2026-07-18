package com.nfm.comsol.model;

import com.comsol.model.Model;
import com.nfm.comsol.config.SimulationConfig;
import com.nfm.comsol.util.ComsolTagUtils;

public final class MeshBuilder {
    public void build(Model model, SimulationConfig config, boolean smokeTest) {
        var component = model.component(ComsolTagUtils.COMPONENT);
        component.mesh().create(ComsolTagUtils.MESH);
        var mesh = component.mesh(ComsolTagUtils.MESH);
        mesh.create("size_particle", "Size");
        mesh.feature("size_particle").selection().geom(ComsolTagUtils.GEOMETRY, 2);
        mesh.feature("size_particle").selection().named(ComsolTagUtils.PARTICLE_DOMAIN);
        mesh.feature("size_particle").set("custom", "on");
        mesh.feature("size_particle").set("hmax", smokeTest ? config.smokeMeshSize() : hmax(config));
        mesh.feature("size_particle").set("hmin", config.meshMinSize());
        mesh.feature("size_particle").set("hgrad", Double.toString(config.meshGrowthRate()));

        mesh.create("surface_size", "Size");
        mesh.feature("surface_size").selection().geom(ComsolTagUtils.GEOMETRY, 1);
        mesh.feature("surface_size").selection().named(ComsolTagUtils.SURFACE_BOUNDARY);
        mesh.feature("surface_size").set("custom", "on");
        mesh.feature("surface_size").set("hmax", smokeTest ? config.smokeMeshSize() : "0.5*(" + hmax(config) + ")");

        mesh.create("free_triangles", "FreeTri");
        mesh.feature("free_triangles").selection().geom(ComsolTagUtils.GEOMETRY, 2);
        mesh.feature("free_triangles").selection().named(ComsolTagUtils.PARTICLE_DOMAIN);
        mesh.run();
    }

    private String hmax(SimulationConfig c) {
        return switch (c.meshLevel()) {
            case "coarse" -> "2*(" + c.meshMaxSize() + ")";
            case "fine" -> "0.67*(" + c.meshMaxSize() + ")";
            case "extra_fine" -> "0.5*(" + c.meshMaxSize() + ")";
            default -> c.meshMaxSize();
        };
    }
}
