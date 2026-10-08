package com.fraudshield.exception;

/** Thrown when a submitted transaction fails validation (bad amount, missing receiver, etc.). */
public class InvalidTransactionException extends Exception {
    private static final long serialVersionUID = 1L;

    public InvalidTransactionException(String message) {
        super(message);
    }

    public InvalidTransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
