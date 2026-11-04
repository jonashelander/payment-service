package com.example.payment_service.exception;

public class DuplicateMerchantException extends RuntimeException {

    public DuplicateMerchantException(String name) {
        super("Merchant with name '" + name + "' already exists");
    }
}
