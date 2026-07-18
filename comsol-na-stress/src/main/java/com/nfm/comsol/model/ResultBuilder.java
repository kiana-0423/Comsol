package com.nfm.comsol.model;

import com.comsol.model.Model;
import com.nfm.comsol.config.SimulationConfig;
import com.nfm.comsol.util.ComsolTagUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/** Central expression map; verify stress symbols in COMSOL 6.4 GUI before production. */
public final class ResultBuilder {
    private final Map<String, String> expressions = new LinkedHashMap<>();

    public ResultBuilder() {
        expressions.put("time_s", "t/1[s]");
        expressions.put("progress", "if(runSign<0,t/tCharge,t/tDischarge)");
        expressions.put("average_xNa", "ave_particle(xNa)");
        expressions.put("surface_xNa", "ave_surface(xNa)");
        expressions.put("center_xNa", "at2(0,0,xNa)");
        expressions.put("surface_center_delta_xNa", "ave_surface(xNa)-at2(0,0,xNa)");
        expressions.put("maximum_von_mises_Pa", "max_particle(solid.mises)");
        expressions.put("average_von_mises_Pa", "ave_particle(solid.mises)");
        expressions.put("stress_stddev_Pa", "sqrt(ave_particle((solid.mises-ave_particle(solid.mises))^2))");
        expressions.put("maximum_principal_stress_Pa", "max_particle(solid.sp1)");
        // VERIFY_WITH_GUI: COMSOL 6.4 axisymmetric cylindrical stress variables.
        expressions.put("radial_stress_center_Pa", "at2(0,0,solid.sr)");
        expressions.put("radial_stress_surface_Pa", "ave_surface(solid.sr)");
        expressions.put("hoop_stress_center_Pa", "at2(0,0,solid.sphi)");
        expressions.put("hoop_stress_surface_Pa", "ave_surface(solid.sphi)");
    }

    public void build(Model model, SimulationConfig config) {
        model.result().dataset().create(ComsolTagUtils.DATASET_CUTLINE, "CutLine2D");
        var cut = model.result().dataset(ComsolTagUtils.DATASET_CUTLINE);
        cut.set("point1", new String[]{"0", "0"});
        cut.set("point2", new String[]{"Rp", "0"});
        cut.set("numpoints", 101);

        createSurfacePlot(model, "plot_concentration", "xNa", "1", "Normalized Na content",
                Double.toString(config.concentrationScaleMin()), Double.toString(config.concentrationScaleMax()));
        createSurfacePlot(model, "plot_stress", "solid.mises", "MPa", "von Mises stress (MPa)",
                config.stressScaleMin(), config.stressScaleMax());
    }

    private void createSurfacePlot(Model model, String tag, String expr, String unit, String label, String min, String max) {
        model.result().create(tag, "PlotGroup2D");
        model.result(tag).label(label);
        model.result(tag).create("surface", "Surface");
        model.result(tag).feature("surface").set("expr", expr);
        model.result(tag).feature("surface").set("unit", unit);
        model.result(tag).feature("surface").set("rangecoloractive", "on");
        model.result(tag).feature("surface").set("rangecolormin", min);
        model.result(tag).feature("surface").set("rangecolormax", max);
    }

    public Map<String, String> expressions() { return Map.copyOf(expressions); }

    public void bindSolution(Model model, String mode) {
        String dataset = mode.equals("discharge") ? "dset2" : "dset1";
        // Automatic study.run() creates dset1 for charge and dset2 for the subsequent discharge study.
        model.result().dataset(ComsolTagUtils.DATASET_CUTLINE).set("data", dataset);
        model.result("plot_concentration").set("data", dataset);
        model.result("plot_stress").set("data", dataset);
    }
}
