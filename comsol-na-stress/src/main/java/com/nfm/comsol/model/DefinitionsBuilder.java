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
        model.component(ComsolTagUtils.COMPONENT).variable("coupling_variables").selection().named(ComsolTagUtils.PARTICLE_DOMAIN);
        model.component(ComsolTagUtils.COMPONENT).variable("coupling_variables").set("xNa", "cNa/csmax", "Normalized Na content");
        model.component(ComsolTagUtils.COMPONENT).variable("coupling_variables").set("rNorm", "min(1,sqrt(r^2+z^2)/Rp)", "Normalized radius");

        String dsByX = "if(runSign<0,Ds_charge(xNa),Ds_discharge(xNa))";
        String dsBase = material.diffusionMode().equals("interpolation") ? dsByX : "DsConst";
        String ds = material.gradientEnabled()
                ? "DsCore+(DsSurface-DsCore)*rNorm^gradExponent" : dsBase;
        model.component(ComsolTagUtils.COMPONENT).variable("coupling_variables").set("DsEffective", ds, "Active diffusivity expression");

        String beta = material.gradientEnabled()
                ? "betaCore+(betaSurface-betaCore)*rNorm^gradExponent" : "betaLinear";
        model.component(ComsolTagUtils.COMPONENT).variable("coupling_variables").set("betaEffective", beta, "Active isotropic expansion coefficient");
        model.component(ComsolTagUtils.COMPONENT).variable("coupling_variables").set("deltaX", "xNa-xInitial", "Na fraction relative to stress-free initial state");
        model.component(ComsolTagUtils.COMPONENT).variable("coupling_variables").set("phaseCoordinate",
                "min(1,max(0,(phaseXHigh-xNa)/max(phaseXHigh-phaseXLow,phaseSmooth)))",
                "0 above phase x-high and 1 below phase x-low during decreasing-x charge");
        model.component(ComsolTagUtils.COMPONENT).variable("coupling_variables").set("phaseProgress", "phaseCoordinate^2*(3-2*phaseCoordinate)",
                "C1-smooth phase progress across the configured x window");

        String strain;
        if ("interpolation".equals(material.strainMode())) {
            strain = material.name().equals("NFM") ? "strain_NFM(xNa)" : "strain_NFMZC(xNa)";
        } else if ("phase_transition".equals(material.strainMode())) {
            strain = "betaEffective*deltaX+phaseExtraStrain*phaseProgress";
        } else {
            strain = "betaEffective*(cNa-cRef)/csmax";
        }
        model.component(ComsolTagUtils.COMPONENT).variable("coupling_variables").set("epsilonChem", strain, "Isotropic chemical eigenstrain; not thermal strain");
        model.component(ComsolTagUtils.COMPONENT).variable("coupling_variables").set("signedNaFlux", "runSign*NNa", "Negative on charge, positive on discharge");

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
