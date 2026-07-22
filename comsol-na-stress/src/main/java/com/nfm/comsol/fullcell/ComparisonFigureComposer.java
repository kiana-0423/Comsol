package com.nfm.comsol.fullcell;

import com.nfm.comsol.config.FullCellConfig;
import com.nfm.comsol.config.SimulationConfig;
import com.nfm.comsol.util.PathUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Creates publication-review NFM/NFMZC panels without external image dependencies. */
public final class ComparisonFigureComposer {
    public List<Path> compose(FullCellConfig cell, SimulationConfig simulation,
                              double cRate, String requestedMode) throws IOException {
        List<Path> outputs = new ArrayList<>();
        List<String> phases = requestedMode.equals("cycle") ? List.of("charge", "discharge") : List.of(requestedMode);
        for (String phase : phases) {
            List<Double> voltages = phase.equals("charge") ? cell.chargeSnapshotVoltages() : cell.dischargeSnapshotVoltages();
            for (double voltage : voltages) {
                for (String field : List.of("concentration", "stress")) {
                    String v = Double.toString(voltage).replace('.', 'p');
                    String nfmStem = "FULLCELL_" + PathUtils.caseStem("NFM", requestedMode, cRate);
                    String nfmzcStem = "FULLCELL_" + PathUtils.caseStem("NFMZC", requestedMode, cRate);
                    Path left = simulation.outputRoot().resolve("figures").resolve(
                            nfmStem + "_" + phase + "_" + field + "_V" + v + ".png");
                    Path right = simulation.outputRoot().resolve("figures").resolve(
                            nfmzcStem + "_" + phase + "_" + field + "_V" + v + ".png");
                    if (!Files.isRegularFile(left) || !Files.isRegularFile(right)) continue;
                    String rate = Double.toString(cRate).replace('.', 'p');
                    Path output = simulation.outputRoot().resolve("figures").resolve(
                            "FULLCELL_COMPARE_" + phase + "_" + rate + "C_" + field + "_V" + v + ".png");
                    combine(left, right, output, "NFM", "NFMZC");
                    outputs.add(output);
                }
            }
            for (double soc : cell.snapshotSocFractions()) {
                for (String field : List.of("concentration", "stress")) {
                    String s = Double.toString(soc).replace('.', 'p');
                    String nfmStem = "FULLCELL_" + PathUtils.caseStem("NFM", requestedMode, cRate);
                    String nfmzcStem = "FULLCELL_" + PathUtils.caseStem("NFMZC", requestedMode, cRate);
                    Path left = simulation.outputRoot().resolve("figures").resolve(
                            nfmStem + "_" + phase + "_" + field + "_SOC" + s + ".png");
                    Path right = simulation.outputRoot().resolve("figures").resolve(
                            nfmzcStem + "_" + phase + "_" + field + "_SOC" + s + ".png");
                    if (!Files.isRegularFile(left) || !Files.isRegularFile(right)) continue;
                    String rate = Double.toString(cRate).replace('.', 'p');
                    Path output = simulation.outputRoot().resolve("figures").resolve(
                            "FULLCELL_COMPARE_" + phase + "_" + rate + "C_" + field + "_SOC" + s + ".png");
                    combine(left, right, output, "NFM", "NFMZC");
                    outputs.add(output);
                }
            }
        }
        return outputs;
    }

    private void combine(Path leftFile, Path rightFile, Path output, String leftLabel, String rightLabel) throws IOException {
        BufferedImage left = ImageIO.read(leftFile.toFile());
        BufferedImage right = ImageIO.read(rightFile.toFile());
        if (left == null || right == null) throw new IOException("Cannot decode comparison images");
        int header = 60;
        int width = left.getWidth()+right.getWidth();
        int height = Math.max(left.getHeight(), right.getHeight())+header;
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setColor(Color.WHITE); g.fillRect(0,0,width,height);
            g.drawImage(left,0,header,null); g.drawImage(right,left.getWidth(),header,null);
            g.setColor(Color.BLACK); g.setFont(new Font("SansSerif",Font.BOLD,28));
            g.drawString(leftLabel,24,40); g.drawString(rightLabel,left.getWidth()+24,40);
        } finally { g.dispose(); }
        ImageIO.write(canvas,"png",output.toFile());
    }
}
