package com.nfm.comsol.fullcell;

import com.comsol.model.Model;
import com.nfm.comsol.config.SimulationConfig;
import com.nfm.comsol.config.MaterialConfig;
import com.nfm.comsol.config.FullCellConfig;
import com.nfm.comsol.util.ComsolTagUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/** Shared plots and scalar acceptance metrics for NFM/NFMZC comparisons. */
public final class FullCellResultBuilder {
    private final Map<String, String> metrics = new LinkedHashMap<>();

    public FullCellResultBuilder() {
        metrics.put("time_s", "t/1[s]");
        metrics.put("cell_voltage_V", "cellVoltage/1[V]");
        metrics.put("specific_capacity_mAh_g", "abs(I_app*t)/mp_pos/1[Ah/kg]");
        metrics.put("average_positive_xNa", "ave_pos(xPos)");
        metrics.put("positive_desodiation_fraction",
                "(x_pos_initial-ave_pos(xPos))/(x_pos_initial-x_pos_final_nominal)");
        metrics.put("surface_positive_xNa", "ave_pos_surface(xPos)");
        metrics.put("center_positive_xNa", "at3(x_pos_center,y_pos_center,z_pos_center,xPos)");
        metrics.put("surface_center_delta_xNa", "ave_pos_surface(xPos)-at3(x_pos_center,y_pos_center,z_pos_center,xPos)");
        metrics.put("maximum_von_mises_Pa", "max_pos(solid_full.mises)");
        metrics.put("average_von_mises_Pa", "ave_pos(solid_full.mises)");
        metrics.put("stress_stddev_Pa", "sqrt(ave_pos((solid_full.mises-ave_pos(solid_full.mises))^2))");
        metrics.put("mass_balance_relative_error",
                "abs(totalNaInventory-totalNaReference)/max(abs(totalNaReference),1e-30[mol])");
        metrics.put("negative_capacity_ratio", "negativeCapacityRatio");
        metrics.put("average_electrolyte_concentration_mol_m3", "ave_electrolyte(liion.cl)/1[mol/m^3]");
        metrics.put("quantitative_ready", "quantitativeReady");
    }

    public void build(Model model, SimulationConfig config, MaterialConfig material, FullCellConfig cell) {
        boolean quantitative = material.parameterStatus().equalsIgnoreCase("measured")
                && cell.parameterStatus().equalsIgnoreCase("measured")
                && material.strainMode().equals("interpolation");
        String suffix = quantitative ? "" : " [PROVISIONAL]";
        var result = model.result();
        result.dataset().create(ComsolTagUtils.FULL_DATASET_POSITIVE_CUTLINE, "CutLine3D");
        var cut = result.dataset(ComsolTagUtils.FULL_DATASET_POSITIVE_CUTLINE);
        cut.set("point1", new String[]{"x_pos_center", "y_pos_center", "z_pos_center"});
        cut.set("point2", new String[]{"x_pos_center+Rp_pos", "y_pos_center", "z_pos_center"});
        cut.set("numpoints", 101);
        volumePlot(model, "full_concentration", ComsolTagUtils.POSITIVE_PARTICLE,
                "xPos", "1", "Positive-particle Na fraction" + suffix,
                Double.toString(config.concentrationScaleMin()), Double.toString(config.concentrationScaleMax()));
        volumePlot(model, "full_stress", ComsolTagUtils.POSITIVE_PARTICLE,
                "solid_full.mises", "MPa", "Positive-particle von Mises stress" + suffix,
                config.stressScaleMin(), config.stressScaleMax());
        autoVolumePlot(model, "full_electrolyte", ComsolTagUtils.ELECTROLYTE_DOMAINS,
                "liion.cl", "mol/m^3", "Electrolyte Na-salt concentration" + suffix);
        autoVolumePlot(model, "full_electrolyte_potential", ComsolTagUtils.ELECTROLYTE_DOMAINS,
                "liion.phil", "V", "Electrolyte potential" + suffix);
        autoVolumePlot(model, "full_solid_potential", ComsolTagUtils.BINDER_DOMAINS,
                "liion.phis", "V", "Solid-phase potential" + suffix);
    }

    private void volumePlot(Model model, String tag, String selection, String expression, String unit,
                            String label, String min, String max) {
        model.result().create(tag, "PlotGroup3D");
        model.result(tag).label(label);
        model.result(tag).create("volume", "Volume");
        model.result(tag).feature("volume").selection().named(selection);
        model.result(tag).feature("volume").set("expr", expression);
        model.result(tag).feature("volume").set("unit", unit);
        model.result(tag).feature("volume").set("rangecoloractive", "on");
        model.result(tag).feature("volume").set("rangecolormin", min);
        model.result(tag).feature("volume").set("rangecolormax", max);
    }

    private void autoVolumePlot(Model model, String tag, String selection, String expression,
                                String unit, String label) {
        model.result().create(tag, "PlotGroup3D");
        model.result(tag).label(label);
        model.result(tag).create("volume", "Volume");
        model.result(tag).feature("volume").selection().named(selection);
        model.result(tag).feature("volume").set("expr", expression);
        model.result(tag).feature("volume").set("unit", unit);
    }

    public void bindSolution(Model model, String dataset) {
        model.result().dataset(ComsolTagUtils.FULL_DATASET_POSITIVE_CUTLINE).set("data", dataset);
        model.result("full_concentration").set("data", dataset);
        model.result("full_stress").set("data", dataset);
        model.result("full_electrolyte").set("data", dataset);
        model.result("full_electrolyte_potential").set("data", dataset);
        model.result("full_solid_potential").set("data", dataset);
    }

    public Map<String, String> metrics() { return Map.copyOf(metrics); }
}
