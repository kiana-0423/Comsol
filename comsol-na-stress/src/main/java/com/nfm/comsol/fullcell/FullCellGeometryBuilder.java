package com.nfm.comsol.fullcell;

import com.comsol.model.Model;
import com.nfm.comsol.util.ComsolTagUtils;

import java.util.List;
import java.util.Arrays;

/** Reconstructs the supplied three-block heterogeneous cell with explicit spherical particles. */
public final class FullCellGeometryBuilder {
    public void build(Model model) {
        model.component().create(ComsolTagUtils.FULL_COMPONENT, true);
        model.component(ComsolTagUtils.FULL_COMPONENT).geom().create(ComsolTagUtils.FULL_GEOMETRY, 3);

        block(model, "anode_block", "0", "L_an");
        block(model, "separator_block", "L_an", "L_sep");
        block(model, "cathode_block", "L_an+L_sep", "L_ca");

        sphere(model, "hc_1", "0.267*L_an", "0.267*W_cell", "0.5*H_cell", "Rp_neg_large");
        sphere(model, "hc_2", "0.733*L_an", "0.733*W_cell", "0.5*H_cell", "Rp_neg_large");
        sphere(model, "hc_3", "0.244*L_an", "0.744*W_cell", "0.5*H_cell", "Rp_neg_small");
        sphere(model, "hc_4", "0.756*L_an", "0.244*W_cell", "0.5*H_cell", "Rp_neg_small");
        sphere(model, "positive_particle", "L_an+L_sep+L_ca/2", "W_cell/2", "H_cell/2", "Rp_pos");
        model.component(ComsolTagUtils.FULL_COMPONENT).geom(ComsolTagUtils.FULL_GEOMETRY).run();

        union(model, ComsolTagUtils.NEGATIVE_PARTICLES, 3, Arrays.asList(
                gsel("hc_1", "dom"), gsel("hc_2", "dom"), gsel("hc_3", "dom"), gsel("hc_4", "dom")));
        union(model, ComsolTagUtils.POSITIVE_PARTICLE, 3, Arrays.asList(gsel("positive_particle", "dom")));
        difference(model, ComsolTagUtils.ANODE_MATRIX, 3, gsel("anode_block", "dom"), ComsolTagUtils.NEGATIVE_PARTICLES);
        union(model, ComsolTagUtils.SEPARATOR_DOMAIN, 3, Arrays.asList(gsel("separator_block", "dom")));
        difference(model, ComsolTagUtils.CATHODE_MATRIX, 3, gsel("cathode_block", "dom"), ComsolTagUtils.POSITIVE_PARTICLE);
        union(model, ComsolTagUtils.BINDER_DOMAINS, 3,
                Arrays.asList(ComsolTagUtils.ANODE_MATRIX, ComsolTagUtils.CATHODE_MATRIX));
        union(model, ComsolTagUtils.FULL_CELL_DOMAINS, 3, Arrays.asList(ComsolTagUtils.ANODE_MATRIX,
                ComsolTagUtils.SEPARATOR_DOMAIN, ComsolTagUtils.CATHODE_MATRIX,
                ComsolTagUtils.NEGATIVE_PARTICLES, ComsolTagUtils.POSITIVE_PARTICLE));
        union(model, ComsolTagUtils.ELECTROLYTE_DOMAINS, 3, Arrays.asList(ComsolTagUtils.ANODE_MATRIX,
                ComsolTagUtils.SEPARATOR_DOMAIN, ComsolTagUtils.CATHODE_MATRIX));

        adjacent(model, ComsolTagUtils.NEGATIVE_SURFACES, ComsolTagUtils.NEGATIVE_PARTICLES);
        adjacent(model, ComsolTagUtils.POSITIVE_SURFACE, ComsolTagUtils.POSITIVE_PARTICLE);
        boundaryBox(model, ComsolTagUtils.NEGATIVE_COLLECTOR, "-1e-12", "1e-12", "-1e-12", "W_cell+1e-12", "-1e-12", "H_cell+1e-12");
        boundaryBox(model, ComsolTagUtils.POSITIVE_COLLECTOR, "L_total-1e-12", "L_total+1e-12", "-1e-12", "W_cell+1e-12", "-1e-12", "H_cell+1e-12");
        boundaryBox(model, "side_y0", "-1e-12", "L_total+1e-12", "-1e-12", "1e-12", "-1e-12", "H_cell+1e-12");
        boundaryBox(model, "side_y1", "-1e-12", "L_total+1e-12", "W_cell-1e-12", "W_cell+1e-12", "-1e-12", "H_cell+1e-12");
        boundaryBox(model, "side_z0", "-1e-12", "L_total+1e-12", "-1e-12", "W_cell+1e-12", "-1e-12", "1e-12");
        boundaryBox(model, "side_z1", "-1e-12", "L_total+1e-12", "-1e-12", "W_cell+1e-12", "H_cell-1e-12", "H_cell+1e-12");
        union(model, ComsolTagUtils.LATERAL_BOUNDARIES, 2,
                Arrays.asList("side_y0", "side_y1", "side_z0", "side_z1"));
    }

    private void block(Model model, String tag, String x, String length) {
        model.component(ComsolTagUtils.FULL_COMPONENT).geom(ComsolTagUtils.FULL_GEOMETRY).create(tag, "Block");
        model.component(ComsolTagUtils.FULL_COMPONENT).geom(ComsolTagUtils.FULL_GEOMETRY)
                .feature(tag).set("size", new String[]{length, "W_cell", "H_cell"});
        model.component(ComsolTagUtils.FULL_COMPONENT).geom(ComsolTagUtils.FULL_GEOMETRY)
                .feature(tag).set("pos", new String[]{x, "0", "0"});
        model.component(ComsolTagUtils.FULL_COMPONENT).geom(ComsolTagUtils.FULL_GEOMETRY)
                .feature(tag).set("selresult", "on");
    }

    private void sphere(Model model, String tag, String x, String y, String z, String radius) {
        model.component(ComsolTagUtils.FULL_COMPONENT).geom(ComsolTagUtils.FULL_GEOMETRY).create(tag, "Sphere");
        model.component(ComsolTagUtils.FULL_COMPONENT).geom(ComsolTagUtils.FULL_GEOMETRY)
                .feature(tag).set("r", radius);
        model.component(ComsolTagUtils.FULL_COMPONENT).geom(ComsolTagUtils.FULL_GEOMETRY)
                .feature(tag).set("pos", new String[]{x, y, z});
        model.component(ComsolTagUtils.FULL_COMPONENT).geom(ComsolTagUtils.FULL_GEOMETRY)
                .feature(tag).set("selresult", "on");
    }

    private String gsel(String feature, String level) { return ComsolTagUtils.FULL_GEOMETRY + "_" + feature + "_" + level; }

    private void union(Model model, String tag, int dim, List<String> inputs) {
        model.component(ComsolTagUtils.FULL_COMPONENT).selection().create(tag, "Union");
        model.component(ComsolTagUtils.FULL_COMPONENT).selection(tag).set("entitydim", dim);
        model.component(ComsolTagUtils.FULL_COMPONENT).selection(tag).set("input", inputs.toArray(new String[0]));
    }

    private void difference(Model model, String tag, int dim, String add, String subtract) {
        model.component(ComsolTagUtils.FULL_COMPONENT).selection().create(tag, "Difference");
        model.component(ComsolTagUtils.FULL_COMPONENT).selection(tag).set("entitydim", dim);
        model.component(ComsolTagUtils.FULL_COMPONENT).selection(tag).set("add", new String[]{add});
        model.component(ComsolTagUtils.FULL_COMPONENT).selection(tag).set("subtract", new String[]{subtract});
    }

    private void adjacent(Model model, String tag, String domain) {
        model.component(ComsolTagUtils.FULL_COMPONENT).selection().create(tag, "Adjacent");
        model.component(ComsolTagUtils.FULL_COMPONENT).selection(tag).set("entitydim", 3);
        model.component(ComsolTagUtils.FULL_COMPONENT).selection(tag).set("outputdim", 2);
        model.component(ComsolTagUtils.FULL_COMPONENT).selection(tag).set("input", new String[]{domain});
    }

    private void boundaryBox(Model model, String tag, String xmin, String xmax,
                             String ymin, String ymax, String zmin, String zmax) {
        model.component(ComsolTagUtils.FULL_COMPONENT).selection().create(tag, "Box");
        model.component(ComsolTagUtils.FULL_COMPONENT).selection(tag).set("entitydim", 2);
        model.component(ComsolTagUtils.FULL_COMPONENT).selection(tag).set("xmin", xmin);
        model.component(ComsolTagUtils.FULL_COMPONENT).selection(tag).set("xmax", xmax);
        model.component(ComsolTagUtils.FULL_COMPONENT).selection(tag).set("ymin", ymin);
        model.component(ComsolTagUtils.FULL_COMPONENT).selection(tag).set("ymax", ymax);
        model.component(ComsolTagUtils.FULL_COMPONENT).selection(tag).set("zmin", zmin);
        model.component(ComsolTagUtils.FULL_COMPONENT).selection(tag).set("zmax", zmax);
    }
}
