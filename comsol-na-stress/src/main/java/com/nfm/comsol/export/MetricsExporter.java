package com.nfm.comsol.export;

import com.comsol.model.Model;
import com.nfm.comsol.model.ResultBuilder;
import com.nfm.comsol.util.ComsolTagUtils;
import com.nfm.comsol.util.PathUtils;
import com.nfm.comsol.util.ValidationUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MetricsExporter {
    public ExportedMetrics export(Model model, ResultBuilder resultBuilder, Path csvDir, String stem) throws IOException {
        Map<String, String> map = resultBuilder.expressions();
        List<String> names = new ArrayList<>(map.keySet());
        String[] expr = names.stream().map(map::get).toArray(String[]::new);
        String solutionData = model.result().dataset(ComsolTagUtils.DATASET_CUTLINE).getString("data");

        model.result().table().create("metrics_table", "Table");
        model.result().numerical().create("metrics_eval", "EvalGlobal");
        var eval = model.result().numerical("metrics_eval");
        eval.set("data", solutionData);
        eval.set("expr", expr);
        eval.set("descr", names.toArray(String[]::new));
        eval.set("looplevelinput", "all");
        eval.set("table", "metrics_table");
        eval.setResult();

        Path timeSeries = csvDir.resolve(stem + "_time_series.csv");
        model.result().export().create("time_series_csv", "Table");
        model.result().export("time_series_csv").set("table", "metrics_table");
        model.result().export("time_series_csv").set("filename", PathUtils.comsolPath(timeSeries));
        model.result().export("time_series_csv").run();

        List<Double> finalValues = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            double[] series = evaluateSeries(model, "metric_scalar_" + i, solutionData, expr[i]);
            double value = series.length == 0 ? Double.NaN : series[series.length - 1];
            ValidationUtils.requireFinite(names.get(i), value);
            finalValues.add(value);
        }
        int stressIndex = names.indexOf("maximum_von_mises_Pa");
        double[] stressSeries = evaluateSeries(model, "metric_initial_stress", solutionData, expr[stressIndex]);
        double initialMaximumStress = stressSeries.length == 0 ? Double.NaN : stressSeries[0];

        Path metrics = csvDir.resolve(stem + "_metrics.csv");
        try (BufferedWriter out = Files.newBufferedWriter(metrics, StandardCharsets.UTF_8)) {
            out.write(String.join(",", names));
            out.newLine();
            for (int i = 0; i < finalValues.size(); i++) {
                if (i > 0) out.write(',');
                out.write(Double.toString(finalValues.get(i)));
            }
            out.newLine();
        }
        return new ExportedMetrics(metrics, timeSeries, names, finalValues, initialMaximumStress);
    }

    private double[] evaluateSeries(Model model, String tag, String data, String expression) {
        model.result().numerical().create(tag, "EvalGlobal");
        var numerical = model.result().numerical(tag);
        numerical.set("data", data);
        numerical.set("expr", new String[]{expression});
        numerical.set("looplevelinput", "all");
        double[][] matrix = numerical.getReal();
        List<Double> flattened = new ArrayList<>();
        for (double[] row : matrix) for (double value : row) flattened.add(value);
        return flattened.stream().mapToDouble(Double::doubleValue).toArray();
    }

    public record ExportedMetrics(Path metricsFile, Path timeSeriesFile,
                                  List<String> names, List<Double> finalValues,
                                  double initialMaximumStress) {
        public double value(String name) { return finalValues.get(names.indexOf(name)); }
    }
}
