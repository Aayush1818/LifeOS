package com.lifeos.common.util;

public final class EncodingUtils {

    private EncodingUtils() {}

    /**
     * Sanitizes extracted text by replacing characters that cannot be encoded in WIN1252.
     * Prevents PostgreSQL ERROR: character with byte sequence in encoding "UTF8" has no equivalent in encoding "WIN1252".
     */
    public static String sanitizeForWin1252(String text) {
        if (text == null) {
            return null;
        }
        return text.replace('\uFFFD', ' ')
                   .replace('\u2022', '-') // bullet
                   .replace('\u2013', '-') // en-dash
                   .replace('\u2014', '-') // em-dash
                   .replace('\u2018', '\'') // left single quote
                   .replace('\u2019', '\'') // right single quote
                   .replace('\u201C', '"') // left double quote
                   .replace('\u201D', '"') // right double quote
                   .replaceAll("[^\\x00-\\x7F\\xA0-\\xFF]", " ");
    }

    public static java.util.Map<String, Object> sanitizeMetadata(java.util.Map<String, Object> metadata) {
        if (metadata == null) {
            return new java.util.HashMap<>();
        }
        java.util.Map<String, Object> sanitized = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, Object> entry : metadata.entrySet()) {
            String key = sanitizeForWin1252(entry.getKey());
            Object value = entry.getValue();
            if (value instanceof String s) {
                sanitized.put(key, sanitizeForWin1252(s));
            } else {
                sanitized.put(key, value);
            }
        }
        return sanitized;
    }

    public static java.util.List<String> sanitizeList(java.util.List<String> list) {
        if (list == null) {
            return new java.util.ArrayList<>();
        }
        return list.stream().map(EncodingUtils::sanitizeForWin1252).toList();
    }
}
