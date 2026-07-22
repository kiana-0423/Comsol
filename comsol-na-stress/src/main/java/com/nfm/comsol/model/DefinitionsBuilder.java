package com.nfm.comsol.model;

import com.comsol.model.Model;
import com.nfm.comsol.config.MaterialConfig;
import com.nfm.comsol.util.ComsolTagUtils;
import com.nfm.comsol.util.PathUtils;

public final class DefinitionsBuilder {
    public void build(Model model, MaterialConfig material) {
        java.nio.file.Path data = material.sourceFile().getParent().getParent().resolve("data");
        createInterpolation(model, "Ds_charge", material.chargeDiffusionCsv());
        createInterpolation(model, "Ds_discharge", material.dischargeDiffusionCsv());
        createInterpolation(model, "strain_NFM", material.name().equals("NFM")
                ? material.strainCsv() : data.resolve("strain_nfm_template.csv"));
        createInterpolation(model, "strain_NFMZC", material.name().equals("NFMZC")
                ? material.strainCsv() : data.resolve("strain_nfmzc_template.csv"));

        model.component(ComsolTagUtils.COMPONENT).variable().create("coupling_variables");
        var vars = model.component(ComsolTagUtils.COMPONENT).variable("coupling_variables");
        vars.selection().named(ComsolTagUtils.PARTICLE_DOMAIN);
        vars.set("xNa", "cNa/csmax", "Normalized Na content");
        vars.set("rNorm", "min(1,sqrt(r^2+z^2)/Rp)", "Normalized radius");

        String dsByX = "if(runSign<0,Ds_charge(xNa),Ds_discharge(xNa))";
        String dsBase = material.diffusionMode().equals("interpolation") ? dsByX : "DsConst";
        String ds = material.gradientEnabled()
                ? "DsCore+(DsSurface-DsCore)*rNorm^gradExponent" : dsBase;
        vars.set("DsEffective", ds, "Active diffusivity expression");

        String beta = material.gradientEnabled()
                ? "betaCore+(betaSurface-betaCore)*rNorm^gradExponent" : "betaLinear";
        vars.set("betaEffective", beta, "Active isotropic expansion coefficient");
        vars.set("deltaX", "xNa-xInitial", "Na fraction relative to stress-free initial state");

        String strain = switch (material.strainMode()) {
            case "interpolation" -> (material.name().equals("NFM") ? "strain_NFM(xNa)" : "strain_NFMZC(xNa)");
            case "phase_transition" -> "betaEffective*deltaX+phaseExtraStrain*flc2hs(phaseXStart-xNa,phaseSmooth)";
            default -> "betaEffective*(cNa-cRef)/csmax";
        };
        vars.set("epsilonChem", strain, "Isotropic chemical eigenstrain; not thermal strain");
        vars.set("signedNaFlux", "runSign*NNa", "Negative on charge, positive on discharge");

        model.component(ComsolTagUtils.COMPONENT).cpl().create("ave_particle", "Average");
        model.component(ComsolTagUtils.COMPONENT).cpl("ave_particle").selection().named(ComsolTagUtils.PARTICLE_DOMAIN);
        model.component(ComsolTagUtils.COMPONENT).cpl().create("ave_surface", "Average");
        model.component(ComsolTagUtils.COMPONENT).cpl("ave_surface").selection().named(ComsolTagUtils.SURFACE_BOUNDARY);
        model.component(ComsolTagUtils.COMPONENT).cpl().create("max_particle", "Maximum");
        model.component(ComsolTagUtils.COMPONENT).cpl("max_particle").selection().named(ComsolTagUtils.PARTICLE_DOMAIN);
    }

    private void createInterpolation(Model model, String tag, java.nio.file.Path file) {
        model.func().create(tag, "Interpolation");
        model.func(tag).set("source", "file");
        model.func(tag).set("filename", PathUtils.comsolPath(file));
        model.func(tag).set("nargs", "1");
        model.func(tag).set("argunit", "1");
        if (tag.startsWith("Ds_")) model.func(tag).set("fununit", "m^2/s");
        else model.func(tag).set("fununit", "1");
    }
}
