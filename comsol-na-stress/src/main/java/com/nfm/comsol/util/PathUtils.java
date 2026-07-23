package com.nfm.comsol.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class PathUtils {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private PathUtils() {}

    public static Path detectProjectRoot() {
        String configured = System.getenv("PROJECT_HOME");
        Path root = configured == null || configured.trim().isEmpty() ? Paths.get("") : Paths.get(configured);
        return root.toAbsolutePath().normalize();
    }

    public static void ensureOutputTree(Path root) throws IOException {
        for (String child : new String[]{"mph", "csv", "figures", "logs"}) Files.createDirectories(root.resolve(child));
    }

    public static String caseStem(String material, String mode, double cRate) {
        String rate = Math.rint(cRate) == cRate ? Integer.toString((int)cRate) : Double.toString(cRate);
        return material.toUpperCase(Locale.ROOT) + "_" + mode.toLowerCase(Locale.ROOT) + "_" + rate + "C";
    }

    public static String timestamp() { return LocalDateTime.now().format(STAMP); }
    public static String comsolPath(Path path) { return path.toAbsolutePath().normalize().toString().replace('\\', '/'); }
}
