package com.fraudshield.exception;

/** Thrown when the risk engine cannot analyse a transaction. */
public class FraudAnalysisException extends Exception {
    private static final long serialVersionUID = 1L;

    public FraudAnalysisException(String message) {
        super(message);
    }

    public FraudAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
