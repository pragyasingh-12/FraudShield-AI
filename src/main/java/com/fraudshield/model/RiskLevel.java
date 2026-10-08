package com.fraudshield.model;

/**
 * Risk classification derived from the 0-100 final score.
 * 0-30 LOW, 31-60 MEDIUM, 61-80 HIGH, 81-100 CRITICAL.
 */
public enum RiskLevel {
    LOW("low"), MEDIUM("medium"), HIGH("high"), CRITICAL("critical");

    private final String cssClass;

    RiskLevel(String cssClass) {
        this.cssClass = cssClass;
    }

    public String getCssClass() {
        return cssClass;
    }

    public static RiskLevel fromScore(int score) {
        if (score <= 30) return LOW;
        if (score <= 60) return MEDIUM;
        if (score <= 80) return HIGH;
        return CRITICAL;
    }

    /** Parses a stored value; returns null for blank/unknown input. */
    public static RiskLevel parse(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return RiskLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
