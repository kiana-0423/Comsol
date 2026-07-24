package com.nfm.comsol.fullcell;

import com.comsol.model.Model;
import com.nfm.comsol.config.FullCellConfig;
import com.nfm.comsol.config.MaterialConfig;

/** Defines all geometry, electrochemical, mechanics and acceptance parameters in SI units. */
public final class FullCellParameterBuilder {
    public void build(Model model, MaterialConfig m, FullCellConfig c, double cRate,
                      SensitivityCase sensitivity) {
        model.param().set("L_an_base", c.anodeLength(), "Anode representative-cell length [provisional geometry]");
        model.param().set("L_sep_base", c.separatorLength(), "Separator length [provisional geometry]");
        model.param().set("L_ca_base", c.cathodeLength(), "Cathode representative-cell length [provisional geometry]");
        model.param().set("W_cell_base", c.width());
        model.param().set("H_cell_base", c.height());
        model.param().set("L_an", "L_an_base*radiusScale");
        model.param().set("L_sep", "L_sep_base*radiusScale");
        model.param().set("L_ca", "L_ca_base*radiusScale");
        model.param().set("W_cell", "W_cell_base*radiusScale");
        model.param().set("H_cell", "H_cell_base*radiusScale");
        model.param().set("L_total", "L_an+L_sep+L_ca");
        model.param().set("A_cell", "W_cell*H_cell");
        model.param().set("Rp_pos_base", m.radius());
        model.param().set("Rp_pos", "Rp_pos_base*radiusScale");
        model.param().set("Rp_neg_large_base", c.negativeLargeRadius());
        model.param().set("Rp_neg_small_base", c.negativeSmallRadius());
        model.param().set("Rp_neg_large", "Rp_neg_large_base*radiusScale");
        model.param().set("Rp_neg_small", "Rp_neg_small_base*radiusScale");
        // Three-dimensional staggered hard-carbon layout reconstructed from
        // the supplied reference figure. Keeping the coordinates as model
        // parameters makes the GUI geometry auditable and preserves the same
        // relative layout when radiusScale is used.
        model.param().set("x_hc_1", "0.300*L_an", "Large HC particle 1 center x");
        model.param().set("y_hc_1", "0.300*W_cell", "Large HC particle 1 center y");
        model.param().set("z_hc_1", "0.478*H_cell", "Large HC particle 1 center z");
        model.param().set("x_hc_2", "0.700*L_an", "Large HC particle 2 center x");
        model.param().set("y_hc_2", "0.700*W_cell", "Large HC particle 2 center y");
        model.param().set("z_hc_2", "0.522*H_cell", "Large HC particle 2 center z");
        model.param().set("x_hc_3", "0.244*L_an", "Small HC particle 3 center x");
        model.param().set("y_hc_3", "0.756*W_cell", "Small HC particle 3 center y");
        model.param().set("z_hc_3", "0.244*H_cell", "Small HC particle 3 center z");
        model.param().set("x_hc_4", "0.756*L_an", "Small HC particle 4 center x");
        model.param().set("y_hc_4", "0.244*W_cell", "Small HC particle 4 center y");
        model.param().set("z_hc_4", "0.756*H_cell", "Small HC particle 4 center z");

        model.param().set("rho_pos", m.density());
        // Material property files are shared with the retained 2D particle
        // model, where csmax is expressed as rho_p/M_formula.
        model.param().set("rho_p", "rho_pos");
        model.param().set("M_formula", m.molarMass());
        model.param().set("csmax_pos", m.csmax());
        model.param().set("x_pos_initial", Double.toString(m.initialX()));
        model.param().set("x_pos_final_nominal", Double.toString(m.finalChargeX()));
        model.param().set("x_pos_center", "L_an+L_sep+L_ca/2");
        model.param().set("y_pos_center", "W_cell/2");
        model.param().set("z_pos_center", "H_cell/2");
        model.param().set("cs0_pos", "x_pos_initial*csmax_pos");
        model.param().set("csmax_neg", c.negativeCsmax());
        model.param().set("x_neg_initial", Double.toString(c.negativeInitialX()));
        model.param().set("cs0_neg", "x_neg_initial*csmax_neg");
        model.param().set("Ds_pos_charge", m.chargeDiffusivity());
        model.param().set("Ds_pos_discharge", m.dischargeDiffusivity());
        model.param().set("Ds_neg", c.negativeDiffusivity());
        model.param().set("E_pos_base", m.youngModulus());
        model.param().set("E_pos", "E_pos_base*modulusScale");
        model.param().set("nu_pos_base", Double.toString(m.poissonRatio()));
        model.param().set("nu_pos", "nu_pos_base*poissonScale");
        model.param().set("beta_pos_base", Double.toString(m.beta()));
        model.param().set("beta_pos", "beta_pos_base*strainScale");
        model.param().set("E_neg", c.negativeYoungModulus());
        model.param().set("nu_neg", Double.toString(c.negativePoissonRatio()));
        model.param().set("beta_neg", Double.toString(c.negativeBeta()),
                "Hard-carbon chemical expansion [provisional; inactive in positive-only mechanics]");
        model.param().set("E_binder", c.binderYoungModulus());
        model.param().set("nu_binder", Double.toString(c.binderPoissonRatio()));
        model.param().set("E_separator", c.separatorYoungModulus());
        model.param().set("nu_separator", Double.toString(c.separatorPoissonRatio()));

        model.param().set("cl0", c.electrolyteInitialConcentration());
        model.param().set("sigma_l_const", c.electrolyteConductivity());
        model.param().set("Dl", c.electrolyteDiffusivity());
        model.param().set("sigma_binder", c.binderConductivity());
        model.param().set("tplus", Double.toString(c.transferenceNumber()));
        model.param().set("alpha", Double.toString(c.chargeTransferAlpha()));
        model.param().set("eps_an", Double.toString(c.anodePorosity()));
        model.param().set("eps_ca", Double.toString(c.cathodePorosity()));
        model.param().set("eps_sep", Double.toString(c.separatorPorosity()));
        model.param().set("tau_an", Double.toString(c.anodeTortuosity()));
        model.param().set("tau_ca", Double.toString(c.cathodeTortuosity()));
        model.param().set("tau_sep", Double.toString(c.separatorTortuosity()));
        model.param().set("i0_pos_base", m.exchangeCurrentDensity());
        model.param().set("i0_pos", "i0_pos_base*positiveKineticsScale");

        model.param().set("Capacity", m.capacity());
        model.param().set("Vp_pos", "4*pi*Rp_pos^3/3");
        model.param().set("Vneg_total", "2*4*pi*Rp_neg_large^3/3+2*4*pi*Rp_neg_small^3/3");
        model.param().set("negativeCapacityRatio",
                "csmax_neg*(1-x_neg_initial)*Vneg_total/(csmax_pos*(x_pos_initial-x_pos_final_nominal)*Vp_pos)",
                "Available hard-carbon Na inventory / required positive-particle transfer");
        model.param().set("totalNaReference", "cs0_pos*Vp_pos+cs0_neg*Vneg_total",
                "Initial active-particle Na inventory; electrolyte contribution is tracked as a change from cl0");
        model.param().set("mp_pos", "rho_pos*Vp_pos");
        model.param().set("Q_pos", "mp_pos*Capacity");
        model.param().set("I_1C", "Q_pos/1[h]");
        model.param().set("C_rate", Double.toString(cRate));
        model.param().set("I_app", "C_rate*I_1C");
        model.param().set("j_app", "I_app/A_cell");
        model.param().set("N_pos", "I_app/(F_const*4*pi*Rp_pos^2)");
        model.param().set("N_neg", "I_app/(F_const*(2*4*pi*Rp_neg_large^2+2*4*pi*Rp_neg_small^2))");
        model.param().set("t_to_nominal_x",
                "(x_pos_initial-x_pos_final_nominal)*F_const*csmax_pos*Vp_pos/I_app",
                "Reference time to x=0.2; not a stopping condition");
        model.param().set("t_nominal", "1.2*max(1[h]/C_rate,t_to_nominal_x)",
                "Safety horizon; voltage StopCondition determines the actual endpoint");
        model.param().set("V_charge_cutoff", c.chargeCutoffVoltage() + "[V]");
        model.param().set("V_discharge_cutoff", c.dischargeCutoffVoltage() + "[V]");
        model.param().set("runDirection", "1", "1=charge, -1=discharge");
        model.param().set("useMeasuredStrain", m.strainMode().equals("interpolation") ? "1" : "0",
                "Use XRD-derived strain interpolation only when explicitly selected");
        boolean quantitativeReady = m.parameterStatus().equalsIgnoreCase("measured")
                && c.parameterStatus().equalsIgnoreCase("measured")
                && m.strainMode().equals("interpolation");
        model.param().set("quantitativeReady", quantitativeReady ? "1" : "0",
                "1 only when OCV/mechanics/full-cell inputs and XRD strain are measured");

        model.param().set("diffusionChargeScale", Double.toString(sensitivity.chargeDiffusionScale()));
        model.param().set("diffusionDischargeScale", Double.toString(sensitivity.dischargeDiffusionScale()));
        model.param().set("strainScale", Double.toString(sensitivity.strainScale()));
        model.param().set("modulusScale", Double.toString(sensitivity.modulusScale()));
        model.param().set("poissonScale", Double.toString(sensitivity.poissonScale()));
        model.param().set("radiusScale", Double.toString(sensitivity.radiusScale()));
        model.param().set("positiveKineticsScale", Double.toString(sensitivity.positiveKineticsScale()));
        model.param().set("negativeKineticsScale", Double.toString(sensitivity.negativeKineticsScale()));
    }
}
