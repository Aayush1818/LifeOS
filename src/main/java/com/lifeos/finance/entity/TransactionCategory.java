package com.lifeos.finance.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TransactionCategory {
    HOUSING,
    FOOD_DINING,
    UTILITIES,
    TRANSPORTATION,
    HEALTHCARE,
    ENTERTAINMENT,
    SHOPPING,
    FINANCIAL_OBLIGATIONS,
    PERSONAL_CARE,
    EDUCATION,
    TRAVEL,
    INCOME_SALARY,
    INCOME_INVESTMENT,
    INCOME_FREELANCE,
    INCOME_OTHER,
    OTHER;

    @JsonCreator
    public static TransactionCategory fromString(String value) {
        if (value == null || value.isBlank()) {
            return OTHER;
        }
        String normalized = value.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        return switch (normalized) {
            case "SALARY", "INCOME_SALARY" -> INCOME_SALARY;
            case "INVESTMENT", "INCOME_INVESTMENT" -> INCOME_INVESTMENT;
            case "FREELANCE", "INCOME_FREELANCE" -> INCOME_FREELANCE;
            case "GROCERIES", "DINING_OUT", "FOOD", "DINING", "FOOD_DINING" -> FOOD_DINING;
            case "BILLS", "UTILITIES" -> UTILITIES;
            case "RENT", "MORTGAGE", "HOUSING" -> HOUSING;
            case "TRANSIT", "TRANSPORT", "TRANSPORTATION" -> TRANSPORTATION;
            case "HEALTH", "MEDICAL", "HEALTHCARE" -> HEALTHCARE;
            case "LEISURE", "FUN", "ENTERTAINMENT" -> ENTERTAINMENT;
            case "OBLIGATIONS", "DEBT", "LOANS", "FINANCIAL_OBLIGATIONS" -> FINANCIAL_OBLIGATIONS;
            case "SELF_CARE", "PERSONAL_CARE" -> PERSONAL_CARE;
            case "STUDY", "EDUCATION" -> EDUCATION;
            case "TRIP", "VACATION", "TRAVEL" -> TRAVEL;
            case "SHOPPING" -> SHOPPING;
            default -> {
                try {
                    yield TransactionCategory.valueOf(normalized);
                } catch (IllegalArgumentException e) {
                    yield OTHER;
                }
            }
        };
    }
}
