package com.nfm.comsol.fullcell;

import com.comsol.model.Model;
import com.nfm.comsol.config.SimulationConfig;
import com.nfm.comsol.config.MaterialConfig;
import com.nfm.comsol.config.FullCellConfig;
import com.nfm.comsol.util.ComsolTagUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;

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
        metrics.put("maximum_von_mises_Pa", "max_pos(solid.mises)");
        metrics.put("average_von_mises_Pa", "ave_pos(solid.mises)");
        metrics.put("stress_stddev_Pa", "sqrt(ave_pos((solid.mises-ave_pos(solid.mises))^2))");
        metrics.put("mass_balance_relative_error",
                "abs(totalNaInventory-totalNaReference)/max(abs(totalNaReference),1e-30[mol])");
        metrics.put("negative_capacity_ratio", "negativeCapacityRatio");
        metrics.put("average_electrolyte_concentration_mol_m3", "ave_electrolyte(cl)/1[mol/m^3]");
        metrics.put("quantitative_ready", "quantitativeReady");
    }

    public void build(Model model, SimulationConfig config, MaterialConfig material, FullCellConfig cell) {
        boolean quantitative = material.parameterStatus().equalsIgnoreCase("measured")
                && cell.parameterStatus().equalsIgnoreCase("measured")
                && material.strainMode().equals("interpolation");
        String suffix = quantitative ? "" : " [PROVISIONAL]";
        model.result().dataset().create(ComsolTagUtils.FULL_DATASET_POSITIVE_CUTLINE, "CutLine3D");
        model.result().dataset(ComsolTagUtils.FULL_DATASET_POSITIVE_CUTLINE)
                .set("genpoints", new String[][]{
                        {"x_pos_center", "y_pos_center", "z_pos_center"},
                        {"x_pos_center+Rp_pos", "y_pos_center", "z_pos_center"}});
        volumePlot(model, "full_concentration", ComsolTagUtils.POSITIVE_PARTICLE,
                "xPos", "1", "Positive-particle Na fraction" + suffix,
                Double.toString(config.concentrationScaleMin()), Double.toString(config.concentrationScaleMax()));
        volumePlot(model, "full_stress", ComsolTagUtils.POSITIVE_PARTICLE,
                "solid.mises", "MPa", "Positive-particle von Mises stress" + suffix,
                config.stressScaleMin(), config.stressScaleMax());
        autoVolumePlot(model, "full_electrolyte", ComsolTagUtils.ELECTROLYTE_DOMAINS,
                "cl", "mol/m^3", "Electrolyte Na-salt concentration" + suffix);
        autoVolumePlot(model, "full_electrolyte_potential", ComsolTagUtils.ELECTROLYTE_DOMAINS,
                "phil", "V", "Electrolyte potential" + suffix);
        autoVolumePlot(model, "full_solid_potential", ComsolTagUtils.BINDER_DOMAINS,
                "phis", "V", "Solid-phase potential" + suffix);
    }

    private void volumePlot(Model model, String tag, String selection, String expression, String unit,
                            String label, String min, String max) {
        model.result().create(tag, "PlotGroup3D");
        model.result(tag).label(label);
        model.result(tag).create("volume", "Volume");
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
        model.result(tag).feature("volume").set("expr", expression);
        model.result(tag).feature("volume").set("unit", unit);
    }

    public void bindSolution(Model model, String dataset) {
        model.result().dataset(ComsolTagUtils.FULL_DATASET_POSITIVE_CUTLINE).set("data", dataset);
        // Also refresh expressions when binding a model loaded for --export-only;
        // older saved MPH files may contain pre-6.4-debugging variable names.
        model.result("full_stress").feature("volume").set("expr", "solid.mises");
        model.result("full_electrolyte").feature("volume").set("expr", "cl");
        model.result("full_electrolyte_potential").feature("volume").set("expr", "phil");
        model.result("full_solid_potential").feature("volume").set("expr", "phis");
        model.result("full_concentration").set("data", dataset);
        model.result("full_stress").set("data", dataset);
        model.result("full_electrolyte").set("data", dataset);
        model.result("full_electrolyte_potential").set("data", dataset);
        model.result("full_solid_potential").set("data", dataset);
        model.result("full_concentration").selection().named(ComsolTagUtils.POSITIVE_PARTICLE);
        model.result("full_stress").selection().named(ComsolTagUtils.POSITIVE_PARTICLE);
        model.result("full_electrolyte").selection().named(ComsolTagUtils.ELECTROLYTE_DOMAINS);
        model.result("full_electrolyte_potential").selection().named(ComsolTagUtils.ELECTROLYTE_DOMAINS);
        model.result("full_solid_potential").selection().named(ComsolTagUtils.BINDER_DOMAINS);
    }

    public Map<String, String> metrics() {
        return Collections.unmodifiableMap(new LinkedHashMap<String, String>(metrics));
    }
}
