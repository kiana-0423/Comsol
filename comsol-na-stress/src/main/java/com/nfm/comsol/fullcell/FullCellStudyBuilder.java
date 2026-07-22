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
        var time = model.study(tag).feature("time");
        time.set("tlist", tlist);
        time.set("usertol", true);
        time.set("rtol", Double.toString(config.relativeTolerance()));
        time.set("plot", "off");
    }

    private void activate(Model model, String study, String step, boolean battery, boolean diffusion, boolean solid) {
        var feature = model.study(study).feature(step);
        feature.activate(ComsolTagUtils.FULL_BATTERY, battery);
        feature.activate(ComsolTagUtils.POSITIVE_DIFFUSION, diffusion);
        feature.activate(ComsolTagUtils.NEGATIVE_DIFFUSION, diffusion);
        feature.activate(ComsolTagUtils.FULL_SOLID, solid);
    }

    public void prepareAndRun(Model model, String studyTag, String solutionTag,
                              SimulationConfig config, boolean continuation) {
        model.study(studyTag).createAutoSequences("sol");
        var solver = model.sol(solutionTag);
        if (continuation) {
            var time = model.study(studyTag).feature("time");
            time.set("useinitsol", true);
            time.set("initmethod", "sol");
            time.set("initstudy", ComsolTagUtils.FULL_STUDY_CHARGE);
            time.set("solnum", "last");
        }
        var timeSolver = solver.feature("t1");
        timeSolver.set("timemethod", "bdf");
        timeSolver.set("rtol", Double.toString(config.relativeTolerance()));
        timeSolver.set("maxstepconstraintbdf", "expr");
        timeSolver.set("maxstepexpressionbdf", config.maxStepFraction() + "*t_nominal");
        timeSolver.set("tstepsbdf", "intermediate");
        timeSolver.create("stop_voltage", "StopCondition");
        var stop = timeSolver.feature("stop_voltage");
        String condition = studyTag.equals(ComsolTagUtils.FULL_STUDY_CHARGE)
                ? "comp_full.cellVoltage>=V_charge_cutoff"
                : "comp_full.cellVoltage<=V_discharge_cutoff";
        stop.setIndex("stopcondarr", condition, 0);
        stop.setIndex("stopcondActive", "on", 0);
        stop.setIndex("stopcondterminateon", "true", 0);
        stop.setIndex("stopconddesc", "Voltage cutoff for " + studyTag, 0);
        solver.runAll();
    }
}
