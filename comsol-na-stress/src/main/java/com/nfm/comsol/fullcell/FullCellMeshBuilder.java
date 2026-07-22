package com.nfm.comsol.fullcell;

import com.comsol.model.Model;
import com.nfm.comsol.config.SimulationConfig;
import com.nfm.comsol.config.FullCellConfig;
import com.nfm.comsol.util.ComsolTagUtils;

/** Tetrahedral mesh with particle/surface refinement and deterministic convergence levels. */
public final class FullCellMeshBuilder {
    public void build(Model model, SimulationConfig config, FullCellConfig cell, boolean smokeTest) {
        var component = model.component(ComsolTagUtils.FULL_COMPONENT);
        component.mesh().create(ComsolTagUtils.FULL_MESH);
        var mesh = component.mesh(ComsolTagUtils.FULL_MESH);

        mesh.create("size_global", "Size");
        mesh.feature("size_global").selection().geom(ComsolTagUtils.FULL_GEOMETRY, 3);
        mesh.feature("size_global").selection().named(ComsolTagUtils.FULL_CELL_DOMAINS);
        mesh.feature("size_global").set("custom", "on");
        mesh.feature("size_global").set("hmax", smokeTest ? "1.5[um]" : hmax(config, cell));
        mesh.feature("size_global").set("hmin", smokeTest ? "0.2[um]" : cell.meshMinSize());
        mesh.feature("size_global").set("hgrad", Double.toString(config.meshGrowthRate()));

        mesh.create("size_particles", "Size");
        mesh.feature("size_particles").selection().geom(ComsolTagUtils.FULL_GEOMETRY, 3);
        mesh.feature("size_particles").selection().named(ComsolTagUtils.POSITIVE_PARTICLE);
        mesh.feature("size_particles").set("custom", "on");
        mesh.feature("size_particles").set("hmax", smokeTest ? "0.75[um]" : "0.5*(" + hmax(config, cell) + ")");
        mesh.feature("size_particles").set("hmin", smokeTest ? "0.15[um]" : cell.meshMinSize());

        mesh.create("size_interfaces", "Size");
        mesh.feature("size_interfaces").selection().geom(ComsolTagUtils.FULL_GEOMETRY, 2);
        mesh.feature("size_interfaces").selection().named(ComsolTagUtils.POSITIVE_SURFACE);
        mesh.feature("size_interfaces").set("custom", "on");
        mesh.feature("size_interfaces").set("hmax", smokeTest ? "0.5[um]" : "0.35*(" + hmax(config, cell) + ")");

        mesh.create("free_tet", "FreeTet");
        mesh.feature("free_tet").selection().geom(ComsolTagUtils.FULL_GEOMETRY, 3);
        mesh.feature("free_tet").selection().named(ComsolTagUtils.FULL_CELL_DOMAINS);
        mesh.run();
    }

    private String hmax(SimulationConfig c, FullCellConfig cell) {
        return switch (c.meshLevel()) {
            case "coarse" -> "2*(" + cell.meshMaxSize() + ")";
            case "fine" -> "0.67*(" + cell.meshMaxSize() + ")";
            case "extra_fine" -> "0.5*(" + cell.meshMaxSize() + ")";
            default -> cell.meshMaxSize();
        };
    }
}
