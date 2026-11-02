package com.example.payment_service.exception;

import com.example.payment_service.model.Transaction;

public class ChargeFailedException extends RuntimeException {
    private final Transaction transaction;

    public ChargeFailedException(Transaction transaction, Throwable cause) {
        super(cause.getMessage(), cause);
        this.transaction = transaction;
    }

    public Transaction getTransaction() {
        return transaction;
    }
}
