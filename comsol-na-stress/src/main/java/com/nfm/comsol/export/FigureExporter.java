package com.nfm.comsol.export;

import com.comsol.model.Model;
import com.nfm.comsol.config.SimulationConfig;
import com.nfm.comsol.util.PathUtils;

import java.nio.file.Path;

public final class FigureExporter {
    public void export(Model model, SimulationConfig config, Path figureDir, String stem) {
        for (double progress : config.exportFractions()) {
            int percent = (int)Math.round(progress * 100);
            int level = nearestLevel(config, progress) + 1; // COMSOL solution levels are 1-based.
            exportOne(model, "plot_concentration", "img_conc_" + percent, level,
                    figureDir.resolve(stem + "_concentration_SOC" + percent + ".png"));
            exportOne(model, "plot_stress", "img_stress_" + percent, level,
                    figureDir.resolve(stem + "_stress_SOC" + percent + ".png"));
        }
    }

    private void exportOne(Model model, String plotTag, String exportTag, int level, Path file) {
        model.result(plotTag).set("looplevelinput", "manual");
        model.result(plotTag).set("looplevel", new int[]{level});
        model.result().export().create(exportTag, "Image2D");
        var image = model.result().export(exportTag);
        image.set("plotgroup", plotTag);
        image.set("pngfilename", PathUtils.comsolPath(file));
        image.set("width", 1200);
        image.set("height", 1000);
        image.set("resolution", 150);
        image.run();
    }

    private int nearestLevel(SimulationConfig config, double target) {
        int best = 0;
        double distance = Double.POSITIVE_INFINITY;
        for (int i = 0; i < config.outputFractions().size(); i++) {
            double d = Math.abs(config.outputFractions().get(i) - target);
            if (d < distance) { best = i; distance = d; }
        }
        return best;
    }
}
