package com.fiap.bank.atm.application.exception;

public class AtmOperationException extends RuntimeException {

    public enum Reason {
        ACCOUNT_BLOCKED,
        INVALID_PIN,
        INSUFFICIENT_FUNDS,
        DAILY_LIMIT_EXCEEDED,
        INVALID_OPERATION
    }

    private final Reason reason;

    public AtmOperationException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public AtmOperationException(Reason reason, Throwable cause) {
        super(cause.getMessage(), cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
