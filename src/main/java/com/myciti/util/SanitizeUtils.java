package com.myciti.util;

import java.util.regex.Pattern;

/**
 * Utility methods for string input sanitization across backend services.
 */
public final class SanitizeUtils {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");

    private SanitizeUtils() {
        // Prevent instantiation
    }

    /**
     * Strips HTML tags and trims whitespace. Returns null if empty.
     */
    public static String cleanText(String input) {
        if (input == null) return null;
        String cleaned = HTML_TAG_PATTERN.matcher(input).replaceAll("").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    /**
     * Strips HTML tags, trims whitespace, and clamps length to maxLength.
     */
    public static String cleanText(String input, int maxLength) {
        if (input == null) return null;
        String cleaned = HTML_TAG_PATTERN.matcher(input).replaceAll("").trim();
        if (cleaned.length() > maxLength) {
            return cleaned.substring(0, maxLength);
        }
        return cleaned;
    }
}
