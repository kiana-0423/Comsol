package com.nfm.comsol.model;

import com.comsol.model.Model;
import com.nfm.comsol.util.ComsolTagUtils;

/** Builds one exact semicircle in the r-z plane and robust named selections. */
public final class GeometryBuilder {
    public void build(Model model) {
        model.component().create(ComsolTagUtils.COMPONENT, true);
        model.component(ComsolTagUtils.COMPONENT).geom().create(ComsolTagUtils.GEOMETRY, 2);
        model.component(ComsolTagUtils.COMPONENT).geom(ComsolTagUtils.GEOMETRY).axisymmetric(true);
        // COMSOL 6.4 Circle with angle=180 and rot=-90 is the r>=0 semicircular sector.
        // VERIFY_WITH_GUI: confirm the generated sector is exactly bounded by r=0.
        model.component(ComsolTagUtils.COMPONENT).geom(ComsolTagUtils.GEOMETRY)
                .create("particle_semicircle", "Circle");
        model.component(ComsolTagUtils.COMPONENT).geom(ComsolTagUtils.GEOMETRY)
                .feature("particle_semicircle").set("r", "Rp");
        model.component(ComsolTagUtils.COMPONENT).geom(ComsolTagUtils.GEOMETRY)
                .feature("particle_semicircle").set("angle", "180");
        model.component(ComsolTagUtils.COMPONENT).geom(ComsolTagUtils.GEOMETRY)
                .feature("particle_semicircle").set("rot", "-90");
        model.component(ComsolTagUtils.COMPONENT).geom(ComsolTagUtils.GEOMETRY)
                .feature("particle_semicircle").set("selresult", "on");
        model.component(ComsolTagUtils.COMPONENT).geom(ComsolTagUtils.GEOMETRY).run();

        model.component(ComsolTagUtils.COMPONENT).selection().create(ComsolTagUtils.PARTICLE_DOMAIN, "Union");
        model.component(ComsolTagUtils.COMPONENT).selection(ComsolTagUtils.PARTICLE_DOMAIN).set("entitydim", 2);
        model.component(ComsolTagUtils.COMPONENT).selection(ComsolTagUtils.PARTICLE_DOMAIN).set("input", new String[]{"geom1_particle_semicircle_dom"});

        model.component(ComsolTagUtils.COMPONENT).selection().create("particle_all_boundaries", "Adjacent");
        model.component(ComsolTagUtils.COMPONENT).selection("particle_all_boundaries").set("entitydim", 2);
        model.component(ComsolTagUtils.COMPONENT).selection("particle_all_boundaries").set("outputdim", 1);
        model.component(ComsolTagUtils.COMPONENT).selection("particle_all_boundaries").set("input", new String[]{ComsolTagUtils.PARTICLE_DOMAIN});

        // Coordinate selections avoid depending on generated boundary numbers.
        model.component(ComsolTagUtils.COMPONENT).selection().create(ComsolTagUtils.AXIS_BOUNDARY, "Box");
        model.component(ComsolTagUtils.COMPONENT).selection(ComsolTagUtils.AXIS_BOUNDARY).set("entitydim", 1);
        model.component(ComsolTagUtils.COMPONENT).selection(ComsolTagUtils.AXIS_BOUNDARY).set("xmin", -1e-15);
        model.component(ComsolTagUtils.COMPONENT).selection(ComsolTagUtils.AXIS_BOUNDARY).set("xmax", 1e-15);
        model.component(ComsolTagUtils.COMPONENT).selection(ComsolTagUtils.AXIS_BOUNDARY).set("ymin", -1.0);
        model.component(ComsolTagUtils.COMPONENT).selection(ComsolTagUtils.AXIS_BOUNDARY).set("ymax", 1.0);

        model.component(ComsolTagUtils.COMPONENT).selection().create(ComsolTagUtils.SURFACE_BOUNDARY, "Difference");
        model.component(ComsolTagUtils.COMPONENT).selection(ComsolTagUtils.SURFACE_BOUNDARY).set("entitydim", 1);
        model.component(ComsolTagUtils.COMPONENT).selection(ComsolTagUtils.SURFACE_BOUNDARY).set("add", new String[]{"particle_all_boundaries"});
        model.component(ComsolTagUtils.COMPONENT).selection(ComsolTagUtils.SURFACE_BOUNDARY).set("subtract", new String[]{ComsolTagUtils.AXIS_BOUNDARY});

        model.component(ComsolTagUtils.COMPONENT).selection().create(ComsolTagUtils.FIX_POINT, "Box");
        model.component(ComsolTagUtils.COMPONENT).selection(ComsolTagUtils.FIX_POINT).set("entitydim", 0);
        model.component(ComsolTagUtils.COMPONENT).selection(ComsolTagUtils.FIX_POINT).set("xmin", -1e-15);
        model.component(ComsolTagUtils.COMPONENT).selection(ComsolTagUtils.FIX_POINT).set("xmax", 1e-15);
        model.component(ComsolTagUtils.COMPONENT).selection(ComsolTagUtils.FIX_POINT).set("ymin", -1e-15);
        model.component(ComsolTagUtils.COMPONENT).selection(ComsolTagUtils.FIX_POINT).set("ymax", 1e-15);
    }
}
