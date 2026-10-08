package com.fraudshield.model;

/** Lifecycle of a transaction. PENDING means "stored, analysis not finished yet". */
public enum TransactionStatus {
    PENDING, APPROVED, REVIEW, BLOCKED;

    public String getCssClass() {
        return name().toLowerCase();
    }

    public static TransactionStatus parse(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return TransactionStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
