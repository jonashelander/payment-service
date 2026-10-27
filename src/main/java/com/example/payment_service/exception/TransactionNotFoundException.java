package com.example.payment_service.exception;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(Long id) {
        super("No transaction found: " + id);
    }
}
