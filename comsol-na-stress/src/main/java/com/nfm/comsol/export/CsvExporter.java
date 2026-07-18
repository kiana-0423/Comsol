package com.nfm.comsol.export;

import com.comsol.model.Model;
import com.nfm.comsol.util.ComsolTagUtils;
import com.nfm.comsol.util.PathUtils;

import java.nio.file.Path;

public final class CsvExporter {
    public Path exportRadialProfiles(Model model, Path csvDir, String stem) {
        Path file = csvDir.resolve(stem + "_radial_profiles.csv");
        String tag = "radial_csv";
        model.result().export().create(tag, "Data");
        var export = model.result().export(tag);
        export.set("data", ComsolTagUtils.DATASET_CUTLINE);
        export.set("expr", new String[]{
                "sqrt(r^2+z^2)/Rp", "xNa", "cNa", "solid.mises",
                "solid.sr", "solid.sphi", "epsilonChem"});
        export.set("unit", new String[]{"1", "1", "mol/m^3", "MPa", "MPa", "MPa", "1"});
        export.set("descr", new String[]{
                "radius/Rp", "xNa", "cNa", "von Mises stress", "radial stress",
                "hoop stress", "chemical strain"});
        export.set("filename", PathUtils.comsolPath(file));
        export.set("fullprec", true);
        export.run();
        return file;
    }
}
