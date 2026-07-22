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

        var component = model.component(ComsolTagUtils.FULL_COMPONENT);
        component.variable().create("positive_variables");
        var pos = component.variable("positive_variables");
        pos.selection().named(ComsolTagUtils.POSITIVE_PARTICLE);
        pos.set("xPos", "cPos/csmax_pos", "Positive-particle sodium fraction");
        pos.set("xPosBattery", "if(isdefined(cPos),cPos/csmax_pos,x_pos_initial)");
        String positiveDiffusivity = material.diffusionMode().equals("interpolation")
                ? "if(runDirection>0,Ds_pos_charge_fun(xPos),Ds_pos_discharge_fun(xPos))"
                : "if(runDirection>0,Ds_pos_charge,Ds_pos_discharge)";
        pos.set("DsPos", "(" + positiveDiffusivity
                + ")*if(runDirection>0,diffusionChargeScale,diffusionDischargeScale)");
        pos.set("epsilonChemPos", "if(useMeasuredStrain>0.5,strain_pos_fun(xPos),beta_pos*(xPos-x_pos_initial))");
        pos.set("positiveNaFlux", "-" + ComsolTagUtils.POSITIVE_LOCAL_CURRENT + "/F_const",
                "Local Butler-Volmer current converted to Na molar flux");

        component.variable().create("negative_variables");
        var neg = component.variable("negative_variables");
        neg.selection().named(ComsolTagUtils.NEGATIVE_PARTICLES);
        neg.set("xNeg", "cNeg/csmax_neg", "Hard-carbon sodium fraction");
        neg.set("xNegBattery", "if(isdefined(cNeg),cNeg/csmax_neg,x_neg_initial)");
        neg.set("DsNegEffective", "Ds_neg_fun(xNeg)");
        neg.set("negativeNaFlux", "-" + ComsolTagUtils.NEGATIVE_LOCAL_CURRENT + "/F_const",
                "Local Butler-Volmer current converted to Na molar flux");

        component.cpl().create("ave_pos", "Average");
        component.cpl("ave_pos").selection().named(ComsolTagUtils.POSITIVE_PARTICLE);
        component.cpl().create("max_pos", "Maximum");
        component.cpl("max_pos").selection().named(ComsolTagUtils.POSITIVE_PARTICLE);
        component.cpl().create("ave_pos_surface", "Average");
        component.cpl("ave_pos_surface").selection().named(ComsolTagUtils.POSITIVE_SURFACE);
        component.cpl().create("ave_neg", "Average");
        component.cpl("ave_neg").selection().named(ComsolTagUtils.NEGATIVE_PARTICLES);
        component.cpl().create("ave_electrolyte", "Average");
        component.cpl("ave_electrolyte").selection().named(ComsolTagUtils.ELECTROLYTE_DOMAINS);
        component.cpl().create("int_an_electrolyte", "Integration");
        component.cpl("int_an_electrolyte").selection().named(ComsolTagUtils.ANODE_MATRIX);
        component.cpl().create("int_sep_electrolyte", "Integration");
        component.cpl("int_sep_electrolyte").selection().named(ComsolTagUtils.SEPARATOR_DOMAIN);
        component.cpl().create("int_ca_electrolyte", "Integration");
        component.cpl("int_ca_electrolyte").selection().named(ComsolTagUtils.CATHODE_MATRIX);
        component.cpl().create("ave_positive_collector", "Average");
        component.cpl("ave_positive_collector").selection().named(ComsolTagUtils.POSITIVE_COLLECTOR);
        component.cpl().create("ave_negative_collector", "Average");
        component.cpl("ave_negative_collector").selection().named(ComsolTagUtils.NEGATIVE_COLLECTOR);

        component.variable().create("cell_metrics");
        var metrics = component.variable("cell_metrics");
        metrics.set("cellVoltage", "ave_positive_collector(liion.phis)-ave_negative_collector(liion.phis)", "Terminal voltage");
        metrics.set("cellCapacity", "abs(I_app*t)/3600/1[mAh]", "Transferred cell capacity");
        metrics.set("positiveInventory", "ave_pos(cPos)*Vp_pos", "Positive-particle Na inventory proxy");
        metrics.set("negativeInventory", "ave_neg(cNeg)*(2*4*pi*Rp_neg_large^3/3+2*4*pi*Rp_neg_small^3/3)");
        metrics.set("electrolyteInventoryChange",
                "eps_an*int_an_electrolyte(liion.cl-cl0)+eps_sep*int_sep_electrolyte(liion.cl-cl0)+eps_ca*int_ca_electrolyte(liion.cl-cl0)",
                "Electrolyte Na inventory change relative to the uniform initial salt concentration");
        metrics.set("totalNaInventory", "positiveInventory+negativeInventory+electrolyteInventoryChange");
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
