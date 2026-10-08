package com.fraudshield.exception;

/** Wraps low-level SQLException so upper layers never depend on JDBC types. */
public class DatabaseOperationException extends Exception {
    private static final long serialVersionUID = 1L;

    public DatabaseOperationException(String message) {
        super(message);
    }

    public DatabaseOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
