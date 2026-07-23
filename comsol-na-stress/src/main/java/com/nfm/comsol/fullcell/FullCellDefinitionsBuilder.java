package com.nfm.comsol.fullcell;

import com.comsol.model.Model;
import com.nfm.comsol.config.FullCellConfig;
import com.nfm.comsol.config.MaterialConfig;
import com.nfm.comsol.util.ComsolTagUtils;
import com.nfm.comsol.util.PathUtils;

/** Interpolation data, coupling operators and cross-physics expressions for the full cell. */
public final class FullCellDefinitionsBuilder {
    public void build(Model model, MaterialConfig material, FullCellConfig cell) {
        interpolation(model, "Eeq_pos", material.ocvCsv(), "V");
        interpolation(model, "Ds_pos_charge_fun", material.chargeDiffusionCsv(), "m^2/s");
        interpolation(model, "Ds_pos_discharge_fun", material.dischargeDiffusionCsv(), "m^2/s");
        interpolation(model, "Eeq_neg", cell.negativeOcvCsv(), "V");
        interpolation(model, "i0_neg", cell.negativeKineticsCsv(), "A/m^2");
        interpolation(model, "Ds_neg_fun", cell.negativeDiffusivityCsv(), "m^2/s");
        interpolation(model, "sigma_l_fun", cell.electrolyteConductivityCsv(), "S/m");
        interpolation(model, "strain_pos_fun", material.strainCsv(), "1");

        model.component(ComsolTagUtils.FULL_COMPONENT).variable().create("positive_variables");
        model.component(ComsolTagUtils.FULL_COMPONENT).variable("positive_variables")
                .selection().named(ComsolTagUtils.POSITIVE_PARTICLE);
        model.component(ComsolTagUtils.FULL_COMPONENT).variable("positive_variables")
                .set("xPos", "cPos/csmax_pos", "Positive-particle sodium fraction");
        model.component(ComsolTagUtils.FULL_COMPONENT).variable("positive_variables")
                .set("xPosBattery", "if(isdefined(cPos),cPos/csmax_pos,x_pos_initial)");
        String positiveDiffusivity = material.diffusionMode().equals("interpolation")
                ? "if(runDirection>0,Ds_pos_charge_fun(xPos),Ds_pos_discharge_fun(xPos))"
                : "if(runDirection>0,Ds_pos_charge,Ds_pos_discharge)";
        model.component(ComsolTagUtils.FULL_COMPONENT).variable("positive_variables").set("DsPos", "(" + positiveDiffusivity
                + ")*if(runDirection>0,diffusionChargeScale,diffusionDischargeScale)");
        model.component(ComsolTagUtils.FULL_COMPONENT).variable("positive_variables")
                .set("epsilonChemPos", "if(useMeasuredStrain>0.5,strain_pos_fun(xPos),beta_pos*(xPos-x_pos_initial))");

        model.component(ComsolTagUtils.FULL_COMPONENT).variable().create("positive_surface_variables");
        model.component(ComsolTagUtils.FULL_COMPONENT).variable("positive_surface_variables")
                .selection().named(ComsolTagUtils.POSITIVE_SURFACE);
        model.component(ComsolTagUtils.FULL_COMPONENT).variable("positive_surface_variables")
                .set("positiveNaFlux", "-" + ComsolTagUtils.POSITIVE_LOCAL_CURRENT + "/F_const",
                "Local Butler-Volmer current converted to Na molar flux");

        model.component(ComsolTagUtils.FULL_COMPONENT).variable().create("negative_variables");
        model.component(ComsolTagUtils.FULL_COMPONENT).variable("negative_variables")
                .selection().named(ComsolTagUtils.NEGATIVE_PARTICLES);
        model.component(ComsolTagUtils.FULL_COMPONENT).variable("negative_variables")
                .set("xNeg", "cNeg/csmax_neg", "Hard-carbon sodium fraction");
        model.component(ComsolTagUtils.FULL_COMPONENT).variable("negative_variables")
                .set("xNegBattery", "if(isdefined(cNeg),cNeg/csmax_neg,x_neg_initial)");
        model.component(ComsolTagUtils.FULL_COMPONENT).variable("negative_variables")
                .set("DsNegEffective", "Ds_neg_fun(xNeg)");

        model.component(ComsolTagUtils.FULL_COMPONENT).variable().create("negative_surface_variables");
        model.component(ComsolTagUtils.FULL_COMPONENT).variable("negative_surface_variables")
                .selection().named(ComsolTagUtils.NEGATIVE_SURFACES);
        model.component(ComsolTagUtils.FULL_COMPONENT).variable("negative_surface_variables")
                .set("negativeNaFlux", "-" + ComsolTagUtils.NEGATIVE_LOCAL_CURRENT + "/F_const",
                "Local Butler-Volmer current converted to Na molar flux");

        coupling(model, "ave_pos", "Average", ComsolTagUtils.POSITIVE_PARTICLE);
        coupling(model, "max_pos", "Maximum", ComsolTagUtils.POSITIVE_PARTICLE);
        coupling(model, "ave_pos_surface", "Average", ComsolTagUtils.POSITIVE_SURFACE);
        coupling(model, "ave_neg", "Average", ComsolTagUtils.NEGATIVE_PARTICLES);
        coupling(model, "ave_electrolyte", "Average", ComsolTagUtils.ELECTROLYTE_DOMAINS);
        coupling(model, "int_an_electrolyte", "Integration", ComsolTagUtils.ANODE_MATRIX);
        coupling(model, "int_sep_electrolyte", "Integration", ComsolTagUtils.SEPARATOR_DOMAIN);
        coupling(model, "int_ca_electrolyte", "Integration", ComsolTagUtils.CATHODE_MATRIX);
        coupling(model, "ave_positive_collector", "Average", ComsolTagUtils.POSITIVE_COLLECTOR);
        coupling(model, "ave_negative_collector", "Average", ComsolTagUtils.NEGATIVE_COLLECTOR);

        model.component(ComsolTagUtils.FULL_COMPONENT).variable().create("cell_metrics");
        model.component(ComsolTagUtils.FULL_COMPONENT).variable("cell_metrics")
                .set("cellVoltage", "ave_positive_collector(phis)-ave_negative_collector(phis)", "Terminal voltage");
        model.component(ComsolTagUtils.FULL_COMPONENT).variable("cell_metrics")
                .set("cellCapacity", "abs(I_app*t)/3600/1[mAh]", "Transferred cell capacity");
        model.component(ComsolTagUtils.FULL_COMPONENT).variable("cell_metrics")
                .set("positiveInventory", "ave_pos(cPos)*Vp_pos", "Positive-particle Na inventory proxy");
        model.component(ComsolTagUtils.FULL_COMPONENT).variable("cell_metrics")
                .set("negativeInventory", "ave_neg(cNeg)*(2*4*pi*Rp_neg_large^3/3+2*4*pi*Rp_neg_small^3/3)");
        model.component(ComsolTagUtils.FULL_COMPONENT).variable("cell_metrics").set("electrolyteInventoryChange",
                "eps_an*int_an_electrolyte(cl-cl0)+eps_sep*int_sep_electrolyte(cl-cl0)+eps_ca*int_ca_electrolyte(cl-cl0)",
                "Electrolyte Na inventory change relative to the uniform initial salt concentration");
        model.component(ComsolTagUtils.FULL_COMPONENT).variable("cell_metrics")
                .set("totalNaInventory", "positiveInventory+negativeInventory+electrolyteInventoryChange");
    }

    private void coupling(Model model, String tag, String type, String selection) {
        model.component(ComsolTagUtils.FULL_COMPONENT).cpl().create(tag, type);
        model.component(ComsolTagUtils.FULL_COMPONENT).cpl(tag).selection().named(selection);
    }

    private void interpolation(Model model, String tag, java.nio.file.Path file, String unit) {
        model.func().create(tag, "Interpolation");
        model.func(tag).set("source", "file");
        model.func(tag).set("filename", PathUtils.comsolPath(file));
        model.func(tag).set("nargs", "1");
        model.func(tag).set("argunit", "1");
        model.func(tag).set("fununit", unit);
        model.func(tag).set("extrap", "const");
    }
}
