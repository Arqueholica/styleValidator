package com.docstyler.shared.util;

import java.nio.file.Path;

public final class FileUtils {

    private FileUtils() {}

    public static String getExtension(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot).toLowerCase() : "";
    }

    public static String getBaseName(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(0, dot) : name;
    }

    /** Strips path components and control characters from a filename. */
    public static String sanitize(String filename) {
        if (filename == null || filename.isBlank()) return "unnamed";
        String name = filename;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) name = name.substring(slash + 1);
        name = name.replaceAll("[\\x00-\\x1F]", "").replaceAll("^\\.+", "");
        return name.isBlank() ? "unnamed" : name;
    }
}
