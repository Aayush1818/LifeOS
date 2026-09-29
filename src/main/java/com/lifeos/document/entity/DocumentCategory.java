package com.lifeos.document.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DocumentCategory {
    PERSONAL,
    FINANCIAL,
    LEGAL,
    MEDICAL,
    TRAVEL,
    INSURANCE,
    TAX,
    OTHER;

    @JsonCreator
    public static DocumentCategory fromString(String value) {
        if (value == null || value.isBlank()) {
            return OTHER;
        }
        String normalized = value.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        return switch (normalized) {
            case "FINANCE", "FINANCIAL" -> FINANCIAL;
            case "HEALTHCARE", "HEALTH", "MEDICAL" -> MEDICAL;
            case "IDENTITY", "PERSONAL" -> PERSONAL;
            case "WARRANTY", "INSURANCE" -> INSURANCE;
            case "LEGAL" -> LEGAL;
            case "TRAVEL" -> TRAVEL;
            case "TAX" -> TAX;
            default -> {
                try {
                    yield DocumentCategory.valueOf(normalized);
                } catch (IllegalArgumentException e) {
                    yield OTHER;
                }
            }
        };
    }
}
