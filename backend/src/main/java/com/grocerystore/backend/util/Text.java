package com.grocerystore.backend.util;

public final class Text {

    private Text() {
    }

    public static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public static String likePattern(String value) {
        String escaped = trimToEmpty(value)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
