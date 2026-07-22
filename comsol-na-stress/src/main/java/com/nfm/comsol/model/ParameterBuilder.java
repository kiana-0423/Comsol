package com.nfm.comsol.model;

import com.comsol.model.Model;
import com.nfm.comsol.config.MaterialConfig;

public final class ParameterBuilder {
    public void build(Model model, MaterialConfig m, double cRate, String mode) {
        model.param().set("Rp", m.radius(), "Particle radius [provisional]");
        model.param().set("rho_p", m.density(), "Particle density [provisional]");
        model.param().set("Capacity", m.capacity(), "Specific capacity [provisional]");
        model.param().set("M_formula", m.molarMass(), "Formula molar mass");
        model.param().set("csmax", m.csmax(), "Maximum Na site concentration derived from density/molar mass");
        model.param().set("xInitial", Double.toString(m.initialX()), "Initial Na fraction");
        model.param().set("xFinalCharge", Double.toString(m.finalChargeX()), "Charge target: Na1 to Na0.2 means 0.8 removed");
        model.param().set("cs0", "xInitial*csmax", "Initial sodium concentration");
        model.param().set("cRef", "cs0", "Stress-free reference concentration");
        model.param().set("DsCharge", m.chargeDiffusivity(), "GITT Na extraction diffusivity");
        model.param().set("DsDischarge", m.dischargeDiffusivity(), "GITT Na insertion diffusivity");
        model.param().set("DsConst", m.diffusivityFor(mode), "Direction-specific solid Na diffusivity");
        model.param().set("E_particle", m.youngModulus(), "Young modulus [provisional]");
        model.param().set("nu_particle", Double.toString(m.poissonRatio()), "Poisson ratio [provisional]");
        model.param().set("betaLinear", Double.toString(m.beta()), "Isotropic chemical expansion coefficient [provisional]");
        model.param().set("C_rate", Double.toString(cRate), "Applied C-rate");
        model.param().set("F_const", "96485.33212[C/mol]", "Faraday constant");
        model.param().set("Vp", "4*pi*Rp^3/3", "Full spherical particle volume");
        model.param().set("mp", "rho_p*Vp", "Single-particle mass");
        model.param().set("Qp", "mp*Capacity", "Single-particle capacity; mAh/g is dimensionally Ah/kg");
        model.param().set("I_1C", "Qp/1[h]", "Single-particle 1C current");
        model.param().set("I_app", "C_rate*I_1C", "Applied current magnitude");
        model.param().set("Ap", "4*pi*Rp^2", "Full spherical surface area");
        model.param().set("NNa", "I_app/(F_const*Ap)", "Positive Na molar-flux magnitude, mol/(m^2*s)");
        model.param().set("chargeSign", "-1", "PDE inward-source sign: charge removes Na");
        model.param().set("dischargeSign", "1", "PDE inward-source sign: discharge inserts Na");
        model.param().set("runSign", mode.equals("charge") ? "chargeSign" : "dischargeSign", "Active flux sign");
        model.param().set("deltaXCharge", "xInitial-xFinalCharge", "0.8 mol removed for x=1.0 to 0.2");
        model.param().set("tCharge", "deltaXCharge*F_const*csmax*Vp/I_app",
                "Inventory-consistent time to nominal x=0.2 at the capacity-calibrated current");
        model.param().set("tDischarge", "tCharge", "Matching inventory-consistent discharge duration");
        model.param().set("gradExponent", Double.toString(m.gradientExponent()), "Radial gradient exponent [sensitivity]");
        model.param().set("DsCore", m.gradientDsCore(), "Core diffusivity [provisional]");
        model.param().set("DsSurface", m.gradientDsSurface(), "Surface diffusivity [provisional]");
        model.param().set("betaCore", Double.toString(m.gradientBetaCore()), "Core expansion [provisional]");
        model.param().set("betaSurface", Double.toString(m.gradientBetaSurface()), "Surface expansion [provisional]");
        model.param().set("phaseXHigh", Double.toString(m.phaseHigh()), "Higher-x phase-window boundary [provisional]");
        model.param().set("phaseXLow", Double.toString(m.phaseLow()), "Lower-x phase-window boundary [provisional]");
        model.param().set("phaseExtraStrain", Double.toString(m.phaseExtraStrain()), "Phase extra strain [provisional]");
        model.param().set("phaseSmooth", Double.toString(m.phaseSmoothingWidth()), "Phase smoothing width [provisional]");
    }
}
