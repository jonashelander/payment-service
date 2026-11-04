package com.example.payment_service.exception;

public class MerchantNotFoundException extends RuntimeException {

    public MerchantNotFoundException(Long id) {
        super("No merchant found with id: " + id);
    }

    public MerchantNotFoundException(String name) {
        super("No merchant found with name: " + name);
    }
}
