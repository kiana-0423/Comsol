package com.nfm.comsol.model;

import com.comsol.model.Model;
import com.nfm.comsol.config.SimulationConfig;
import com.nfm.comsol.util.ComsolTagUtils;

import java.util.stream.Collectors;

public final class StudyBuilder {
    public void build(Model model, SimulationConfig config, boolean smokeTest) {
        createTransient(model, ComsolTagUtils.STUDY_CHARGE, "Charge", "tCharge", config, smokeTest);
        createTransient(model, ComsolTagUtils.STUDY_DISCHARGE, "Discharge", "tDischarge", config, smokeTest);
        // Discharge continuation is configured immediately before solve, after a real charge solution exists.
    }

    private void createTransient(Model model, String tag, String label, String duration,
                                 SimulationConfig config, boolean smokeTest) {
        model.study().create(tag);
        model.study(tag).label(label);
        model.study(tag).create("time", "Transient");
        model.study(tag).feature("time").activate(ComsolTagUtils.DIFFUSION, true);
        model.study(tag).feature("time").activate(ComsolTagUtils.SOLID, true);
        String tlist = smokeTest ? "range(0," + config.smokeDuration() + "/4," + config.smokeDuration() + ")"
                : config.outputFractions().stream()
                    .map(v -> Double.toString(v) + "*" + duration)
                    .collect(Collectors.joining(" "));
        model.study(tag).feature("time").set("tlist", tlist);
        model.study(tag).feature("time").set("usertol", true);
        model.study(tag).feature("time").set("rtol", Double.toString(config.relativeTolerance()));
        model.study(tag).feature("time").set("plot", "off");
    }

    public void configureDischargeContinuation(Model model) {
        var time = model.study(ComsolTagUtils.STUDY_DISCHARGE).feature("time");
        time.set("useinitsol", true);
        time.set("initmethod", "sol");
        time.set("initstudy", ComsolTagUtils.STUDY_CHARGE);
        time.set("solnum", "last");
    }

    public void prepareAndRun(Model model, String studyTag, String solutionTag,
                              String duration, SimulationConfig config) {
        model.study(studyTag).createAutoSequences("sol");
        var timeSolver = model.sol(solutionTag).feature("t1");
        timeSolver.set("timemethod", "bdf");
        timeSolver.set("rtol", Double.toString(config.relativeTolerance()));
        timeSolver.set("maxstepconstraintbdf", "expr");
        timeSolver.set("maxstepexpressionbdf", config.maxStepFraction() + "*" + duration);
        timeSolver.set("tstepsbdf", "intermediate");
        model.sol(solutionTag).runAll();
    }
}
