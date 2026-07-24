package com.nfm.comsol.fullcell;

import com.comsol.model.Model;
import com.comsol.model.SolverFeature;
import com.nfm.comsol.config.SimulationConfig;
import com.nfm.comsol.util.ComsolTagUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Initialization followed by charge/discharge; mechanics is one-way and quasi-static in intent. */
public final class FullCellStudyBuilder {
    public void build(Model model, SimulationConfig config, boolean smokeTest,
                      boolean transitionSmokeTest) {
        model.study().create(ComsolTagUtils.FULL_STUDY_INIT);
        model.study(ComsolTagUtils.FULL_STUDY_INIT).label("Current distribution initialization");
        model.study(ComsolTagUtils.FULL_STUDY_INIT).create("init", "CurrentDistributionInitialization");
        activate(model, ComsolTagUtils.FULL_STUDY_INIT, "init", true, false, false);

        // A transition smoke test deliberately performs a complete charge so
        // the short discharge starts from the same cutoff state that failed in
        // the former 1C cycle. The ordinary smoke test keeps both legs short.
        transientStudy(model, ComsolTagUtils.FULL_STUDY_CHARGE, "Charge 2.7-4.3 V",
                config, smokeTest && !transitionSmokeTest, false);
        transientStudy(model, ComsolTagUtils.FULL_STUDY_DISCHARGE, "Discharge 4.3-2.0 V",
                config, smokeTest, transitionSmokeTest);
    }

    private void transientStudy(Model model, String tag, String label, SimulationConfig config,
                                boolean smokeTest, boolean transitionDischarge) {
        model.study().create(tag);
        model.study(tag).label(label);
        if (tag.equals(ComsolTagUtils.FULL_STUDY_CHARGE)) {
            model.study(tag).create("cdi", "CurrentDistributionInitialization");
            activate(model, tag, "cdi", true, false, false);
        }
        model.study(tag).create("time", "Transient");
        activate(model, tag, "time", true, true, true);
        String tlist = transitionDischarge
                ? "range(0,1[s],20[s])"
                : smokeTest ? "range(0,0.005[s],0.01[s])"
                : config.outputFractions().stream()
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

    public String prepareAndRun(Model model, String studyTag, String preferredSolutionTag,
                                SimulationConfig config, boolean continuation, boolean smokeTest) {
        boolean highRate = model.param().evaluate("C_rate") >= 0.5;
        // Charge contains its own current-distribution step so COMSOL can
        // transfer every algebraic state inside one generated solver
        // sequence, matching the 6.4 Battery Module application models.
        // Discharge instead continues from the charge solution.
        if (continuation) {
            model.study(studyTag).feature("time").set("useinitsol", true);
            model.study(studyTag).feature("time").set("initmethod", "sol");
            model.study(studyTag).feature("time").set("initstudy", ComsolTagUtils.FULL_STUDY_CHARGE);
            model.study(studyTag).feature("time").set("solnum", "last");
        }
        Set<String> solutionsBefore = new HashSet<>(Arrays.asList(model.sol().tags()));
        model.study(studyTag).createAutoSequences("sol");
        String solutionTag = findGeneratedTimeSolution(model, solutionsBefore);
        if (solutionTag == null) {
            throw new IllegalStateException("No new transient solution sequence generated for "
                    + studyTag + " (preferred tag was " + preferredSolutionTag + ")");
        }
        System.out.println("Generated transient solution: " + studyTag + " -> " + solutionTag);
        // COMSOL numbers generated solver features globally inside the model:
        // the discharge time solver is commonly t2 rather than t1. Discover it
        // by feature type instead of relying on an auto-generated tag.
        String timeSolverTag = null;
        for (String featureTag : model.sol(solutionTag).feature().tags()) {
            SolverFeature feature = model.sol(solutionTag).feature(featureTag);
            System.out.println("Generated solver feature: " + solutionTag + "/"
                    + featureTag + " (" + feature.getType() + ")");
            configureMumpsInCore(feature);
            if ("Time".equals(feature.getType())) timeSolverTag = featureTag;
        }
        if (timeSolverTag == null) {
            throw new IllegalStateException("No transient solver feature generated for "
                    + studyTag + " in " + solutionTag);
        }
        SolverFeature timeSolver = model.sol(solutionTag).feature(timeSolverTag);
        timeSolver.set("timemethod", "bdf");
        timeSolver.set("rtol",
                smokeTest ? "1e-2" : Double.toString(config.relativeTolerance()));
        timeSolver.set("initialstepbdfactive", "on");
        timeSolver.set("initialstepbdf", "0.01[s]");
        // Charge's preceding CurrentDistributionInitialization already supplies
        // consistent potentials. Discharge starts from a charge solution but
        // reverses current (gently in the runner), so recompute its algebraic
        // potentials before advancing the differential states.
        timeSolver.set("consistent", continuation && highRate ? "on" : "off");
        timeSolver.set("maxstepconstraintbdf", "expr");
        timeSolver.set("maxstepexpressionbdf", config.maxStepFraction() + "*t_nominal");
        timeSolver.set("tstepsbdf", "intermediate");
        timeSolver.create("stop_voltage", "StopCondition");
        String condition = studyTag.equals(ComsolTagUtils.FULL_STUDY_CHARGE)
                ? "comp_full.cellVoltage>=V_charge_cutoff"
                : "comp_full.cellVoltage<=V_discharge_cutoff";
        timeSolver.feature("stop_voltage").setIndex("stopcondarr", condition, 0);
        timeSolver.feature("stop_voltage").setIndex("stopcondActive", "on", 0);
        timeSolver.feature("stop_voltage").setIndex("stopcondterminateon", "true", 0);
        timeSolver.feature("stop_voltage").setIndex("stopconddesc", "Voltage cutoff for " + studyTag, 0);
        // Preserve the last feasible internal step before crossing the voltage
        // cutoff. A step-after charge solution can already lie beyond a
        // concentration bound and is not a robust initial state for discharge;
        // step-before also avoids exporting a discharge voltage overshoot.
        timeSolver.feature("stop_voltage").set("storestopcondsol",
                highRate ? "stepbefore" : "stepafter");
        // The automatically generated segregated groups repeatedly shrink the
        // time step for the strongly coupled particle concentration / battery
        // current equations at 0.1C. A single Newton system is both better
        // scaled and affordable for the solver-qualified baseline mesh.
        for (String childTag : timeSolver.feature().tags()) {
            if ("Segregated".equals(timeSolver.feature(childTag).getType())) {
                timeSolver.feature().remove(childTag);
            }
        }
        String fullyCoupledTag = timeSolver.feature().uniquetag("fc");
        timeSolver.create(fullyCoupledTag, "FullyCoupled");
        timeSolver.feature(fullyCoupledTag).set("maxiter",
                continuation && highRate ? 50 : 25);
        model.sol(solutionTag).runAll();
        return solutionTag;
    }

    private String findGeneratedTimeSolution(Model model, Set<String> solutionsBefore) {
        for (String candidateTag : model.sol().tags()) {
            if (solutionsBefore.contains(candidateTag)) continue;
            for (String featureTag : model.sol(candidateTag).feature().tags()) {
                if ("Time".equals(model.sol(candidateTag).feature(featureTag).getType())) {
                    return candidateTag;
                }
            }
        }
        return null;
    }

    private void configureMumpsInCore(SolverFeature feature) {
        if (feature.hasProperty("ooc")) {
            System.out.println("Configuring in-core direct solver: "
                    + feature.tag() + " (" + feature.getType() + ")");
            if (feature.hasProperty("linsolver")) feature.set("linsolver", "mumps");
            feature.set("ooc", "off");
            if (feature.hasProperty("mumpsblr")) feature.set("mumpsblr", "off");
        }
        for (String childTag : feature.feature().tags()) {
            configureMumpsInCore(feature.feature(childTag));
        }
    }
}
