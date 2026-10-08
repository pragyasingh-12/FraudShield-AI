package com.fraudshield.model;

/** Workflow state of a fraud alert handled by an analyst. */
public enum AlertStatus {
    OPEN, INVESTIGATING, CONFIRMED_FRAUD, FALSE_POSITIVE;

    public String getLabel() {
        return name().replace('_', ' ');
    }

    public static AlertStatus parse(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return AlertStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
