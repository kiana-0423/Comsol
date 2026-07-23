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
        model.result().export(tag).set("data", ComsolTagUtils.DATASET_CUTLINE);
        model.result().export(tag).set("expr", new String[]{
                "sqrt(r^2+z^2)/Rp", "xNa", "cNa", "solid.mises",
                "solid.sr", "solid.sphi", "epsilonChem"});
        model.result().export(tag).set("unit", new String[]{"1", "1", "mol/m^3", "MPa", "MPa", "MPa", "1"});
        model.result().export(tag).set("descr", new String[]{
                "radius/Rp", "xNa", "cNa", "von Mises stress", "radial stress",
                "hoop stress", "chemical strain"});
        model.result().export(tag).set("filename", PathUtils.comsolPath(file));
        model.result().export(tag).set("fullprec", true);
        model.result().export(tag).run();
        return file;
    }
}
