package com.nfm.comsol.runner;

import com.nfm.comsol.config.ConfigLoader;
import com.nfm.comsol.config.MaterialConfig;
import com.nfm.comsol.config.SimulationConfig;
import com.nfm.comsol.util.PathUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class BatchRunner {
    private final SimulationRunner runner = new SimulationRunner();

    public void runAll(SimulationConfig simulation, SimulationRunner.RunOptions base) throws Exception {
        for (String name : Arrays.asList("NFM", "NFMZC")) {
            MaterialConfig material = ConfigLoader.loadMaterial(simulation.projectRoot(), name);
            List<SimulationRunner.RunResult> solved = new ArrayList<>();
            for (double rate : simulation.cRates()) {
                solved.add(runner.run(material, simulation, new SimulationRunner.RunOptions(
                        rate, base.mode(), base.buildOnly(), base.exportOnly(), base.smokeTest())));
            }
            if (!base.buildOnly() && !base.exportOnly() && !base.smokeTest() && solved.size() > 1) {
                double low = Math.abs(solved.get(0).surfaceCenterDelta());
                double high = Math.abs(solved.get(solved.size() - 1).surfaceCenterDelta());
                if (low > high * 1.01) {
                    throw new IllegalStateException("Cross-rate validation failed for " + name
                            + ": low-rate concentration gradient exceeds high-rate gradient");
                }
            }
        }
    }

    public Path runMeshConvergence(MaterialConfig material, SimulationConfig simulation,
                                   double cRate, String mode) throws Exception {
        List<ConvergenceRow> rows = new ArrayList<>();
        Double previousStress = null;
        for (String level : Arrays.asList("normal", "fine", "extra_fine")) {
            SimulationRunner.RunResult result = runner.run(material, simulation.withMeshLevel(level),
                    new SimulationRunner.RunOptions(cRate, mode, false, false, false));
            double change = previousStress == null ? Double.NaN
                    : Math.abs(result.maximumStress() - previousStress) / Math.max(Math.abs(previousStress), 1e-30);
            boolean converged = previousStress != null && change < simulation.meshConvergenceTolerance();
            rows.add(new ConvergenceRow(level, result.elementCount(), result.maximumStress(),
                    result.averageXNa(), result.surfaceCenterDelta(), change, converged));
            previousStress = result.maximumStress();
        }
        Path file = simulation.outputRoot().resolve("csv").resolve(
                PathUtils.caseStem(material.name(), mode, cRate) + "_mesh_convergence.csv");
        writeConvergence(file, rows);
        return file;
    }

    private void writeConvergence(Path file, List<ConvergenceRow> rows) throws IOException {
        try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            out.write("mesh_level,element_count,maximum_von_mises_Pa,average_xNa,surface_center_delta_xNa,relative_peak_stress_change,converged");
            out.newLine();
            for (ConvergenceRow r : rows) {
                out.write(r.level + "," + r.elements + "," + r.maxStress + "," + r.averageX
                        + "," + r.surfaceCenterDelta + "," + r.relativeChange + "," + r.converged);
                out.newLine();
            }
        }
    }

    private static final class ConvergenceRow {
        final String level;
        final int elements;
        final double maxStress, averageX, surfaceCenterDelta, relativeChange;
        final boolean converged;

        ConvergenceRow(String level, int elements, double maxStress, double averageX,
                       double surfaceCenterDelta, double relativeChange, boolean converged) {
            this.level = level;
            this.elements = elements;
            this.maxStress = maxStress;
            this.averageX = averageX;
            this.surfaceCenterDelta = surfaceCenterDelta;
            this.relativeChange = relativeChange;
            this.converged = converged;
        }
    }
}
