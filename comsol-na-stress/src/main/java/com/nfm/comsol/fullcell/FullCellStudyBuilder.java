package com.nfm.comsol.fullcell;

import com.comsol.model.Model;
import com.nfm.comsol.config.SimulationConfig;
import com.nfm.comsol.util.ComsolTagUtils;

import java.util.stream.Collectors;

/** Initialization followed by charge/discharge; mechanics is one-way and quasi-static in intent. */
public final class FullCellStudyBuilder {
    public void build(Model model, SimulationConfig config, boolean smokeTest) {
        model.study().create(ComsolTagUtils.FULL_STUDY_INIT);
        model.study(ComsolTagUtils.FULL_STUDY_INIT).label("Current distribution initialization");
        model.study(ComsolTagUtils.FULL_STUDY_INIT).create("init", "CurrentDistributionInitialization");
        activate(model, ComsolTagUtils.FULL_STUDY_INIT, "init", true, false, false);

        transientStudy(model, ComsolTagUtils.FULL_STUDY_CHARGE, "Charge 2.7-4.3 V", config, smokeTest);
        transientStudy(model, ComsolTagUtils.FULL_STUDY_DISCHARGE, "Discharge 4.3-2.0 V", config, smokeTest);
    }

    private void transientStudy(Model model, String tag, String label, SimulationConfig config, boolean smokeTest) {
        model.study().create(tag);
        model.study(tag).label(label);
        model.study(tag).create("time", "Transient");
        activate(model, tag, "time", true, true, true);
        String tlist = smokeTest ? "range(0,0.5[s],2[s])" : config.outputFractions().stream()
                .map(v -> v + "*t_nominal").collect(Collectors.joining(" "));
        model.study(tag).feature("time").set("tlist", tlist);
        model.study(tag).feature("time").set("usertol", true);
        model.study(tag).feature("time").set("rtol", Double.toString(config.relativeTolerance()));
        model.study(tag).feature("time").set("plot", "off");
    }

    private void activate(Model model, String study, String step, boolean battery, boolean diffusion, boolean solid) {
        model.study(study).feature(step).activate(ComsolTagUtils.FULL_BATTERY, battery);
        model.study(study).feature(step).activate(ComsolTagUtils.POSITIVE_DIFFUSION, diffusion);
        model.study(study).feature(step).activate(ComsolTagUtils.NEGATIVE_DIFFUSION, diffusion);
        model.study(study).feature(step).activate(ComsolTagUtils.FULL_SOLID, solid);
    }

    public void prepareAndRun(Model model, String studyTag, String solutionTag,
                              SimulationConfig config, boolean continuation) {
        model.study(studyTag).createAutoSequences("sol");
        if (continuation) {
            model.study(studyTag).feature("time").set("useinitsol", true);
            model.study(studyTag).feature("time").set("initmethod", "sol");
            model.study(studyTag).feature("time").set("initstudy", ComsolTagUtils.FULL_STUDY_CHARGE);
            model.study(studyTag).feature("time").set("solnum", "last");
        }
        model.sol(solutionTag).feature("t1").set("timemethod", "bdf");
        model.sol(solutionTag).feature("t1").set("rtol", Double.toString(config.relativeTolerance()));
        model.sol(solutionTag).feature("t1").set("maxstepconstraintbdf", "expr");
        model.sol(solutionTag).feature("t1").set("maxstepexpressionbdf", config.maxStepFraction() + "*t_nominal");
        model.sol(solutionTag).feature("t1").set("tstepsbdf", "intermediate");
        model.sol(solutionTag).feature("t1").create("stop_voltage", "StopCondition");
        String condition = studyTag.equals(ComsolTagUtils.FULL_STUDY_CHARGE)
                ? "comp_full.cellVoltage>=V_charge_cutoff"
                : "comp_full.cellVoltage<=V_discharge_cutoff";
        model.sol(solutionTag).feature("t1").feature("stop_voltage").setIndex("stopcondarr", condition, 0);
        model.sol(solutionTag).feature("t1").feature("stop_voltage").setIndex("stopcondActive", "on", 0);
        model.sol(solutionTag).feature("t1").feature("stop_voltage").setIndex("stopcondterminateon", "true", 0);
        model.sol(solutionTag).feature("t1").feature("stop_voltage").setIndex("stopconddesc", "Voltage cutoff for " + studyTag, 0);
        model.sol(solutionTag).runAll();
    }
}
