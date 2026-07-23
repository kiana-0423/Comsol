package com.nfm.comsol.fullcell;

import com.comsol.model.Model;
import com.nfm.comsol.config.SimulationConfig;
import com.nfm.comsol.config.FullCellConfig;
import com.nfm.comsol.util.ComsolTagUtils;

/** Tetrahedral mesh with particle/surface refinement and deterministic convergence levels. */
public final class FullCellMeshBuilder {
    public void build(Model model, SimulationConfig config, FullCellConfig cell, boolean smokeTest) {
        boolean qualifiedBaseline = smokeTest || "normal".equals(config.meshLevel());
        model.component(ComsolTagUtils.FULL_COMPONENT).mesh().create(ComsolTagUtils.FULL_MESH);

        model.component(ComsolTagUtils.FULL_COMPONENT).mesh(ComsolTagUtils.FULL_MESH).create("size_global", "Size");
        model.component(ComsolTagUtils.FULL_COMPONENT).mesh(ComsolTagUtils.FULL_MESH).feature("size_global").selection().geom(ComsolTagUtils.FULL_GEOMETRY, 3);
        model.component(ComsolTagUtils.FULL_COMPONENT).mesh(ComsolTagUtils.FULL_MESH).feature("size_global").selection().named(ComsolTagUtils.FULL_CELL_DOMAINS);
        model.component(ComsolTagUtils.FULL_COMPONENT).mesh(ComsolTagUtils.FULL_MESH).feature("size_global").set("custom", "on");
        model.component(ComsolTagUtils.FULL_COMPONENT).mesh(ComsolTagUtils.FULL_MESH).feature("size_global").set("hmax", qualifiedBaseline ? "3[um]" : hmax(config, cell));
        model.component(ComsolTagUtils.FULL_COMPONENT).mesh(ComsolTagUtils.FULL_MESH).feature("size_global").set("hmin", qualifiedBaseline ? "0.5[um]" : cell.meshMinSize());
        model.component(ComsolTagUtils.FULL_COMPONENT).mesh(ComsolTagUtils.FULL_MESH).feature("size_global").set("hgrad", Double.toString(config.meshGrowthRate()));

        model.component(ComsolTagUtils.FULL_COMPONENT).mesh(ComsolTagUtils.FULL_MESH).create("size_particles", "Size");
        model.component(ComsolTagUtils.FULL_COMPONENT).mesh(ComsolTagUtils.FULL_MESH).feature("size_particles").selection().geom(ComsolTagUtils.FULL_GEOMETRY, 3);
        model.component(ComsolTagUtils.FULL_COMPONENT).mesh(ComsolTagUtils.FULL_MESH).feature("size_particles").selection().named(ComsolTagUtils.POSITIVE_PARTICLE);
        model.component(ComsolTagUtils.FULL_COMPONENT).mesh(ComsolTagUtils.FULL_MESH).feature("size_particles").set("custom", "on");
        model.component(ComsolTagUtils.FULL_COMPONENT).mesh(ComsolTagUtils.FULL_MESH).feature("size_particles").set("hmax", qualifiedBaseline ? "1.5[um]" : "0.5*(" + hmax(config, cell) + ")");
        model.component(ComsolTagUtils.FULL_COMPONENT).mesh(ComsolTagUtils.FULL_MESH).feature("size_particles").set("hmin", qualifiedBaseline ? "0.4[um]" : cell.meshMinSize());

        model.component(ComsolTagUtils.FULL_COMPONENT).mesh(ComsolTagUtils.FULL_MESH).create("size_interfaces", "Size");
        model.component(ComsolTagUtils.FULL_COMPONENT).mesh(ComsolTagUtils.FULL_MESH).feature("size_interfaces").selection().geom(ComsolTagUtils.FULL_GEOMETRY, 2);
        model.component(ComsolTagUtils.FULL_COMPONENT).mesh(ComsolTagUtils.FULL_MESH).feature("size_interfaces").selection().named(ComsolTagUtils.POSITIVE_SURFACE);
        model.component(ComsolTagUtils.FULL_COMPONENT).mesh(ComsolTagUtils.FULL_MESH).feature("size_interfaces").set("custom", "on");
        model.component(ComsolTagUtils.FULL_COMPONENT).mesh(ComsolTagUtils.FULL_MESH).feature("size_interfaces").set(
                "hmax", qualifiedBaseline ? "1.25[um]" : "0.35*(" + hmax(config, cell) + ")");

        model.component(ComsolTagUtils.FULL_COMPONENT).mesh(ComsolTagUtils.FULL_MESH).create("free_tet", "FreeTet");
        model.component(ComsolTagUtils.FULL_COMPONENT).mesh(ComsolTagUtils.FULL_MESH).feature("free_tet").selection().geom(ComsolTagUtils.FULL_GEOMETRY, 3);
        model.component(ComsolTagUtils.FULL_COMPONENT).mesh(ComsolTagUtils.FULL_MESH).feature("free_tet").selection().named(ComsolTagUtils.FULL_CELL_DOMAINS);
        model.component(ComsolTagUtils.FULL_COMPONENT).mesh(ComsolTagUtils.FULL_MESH).run();
    }

    private String hmax(SimulationConfig c, FullCellConfig cell) {
        if ("coarse".equals(c.meshLevel())) return "2*(" + cell.meshMaxSize() + ")";
        if ("fine".equals(c.meshLevel())) return "0.67*(" + cell.meshMaxSize() + ")";
        if ("extra_fine".equals(c.meshLevel())) return "0.5*(" + cell.meshMaxSize() + ")";
        return cell.meshMaxSize();
    }
}
