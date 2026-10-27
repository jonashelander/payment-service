package com.example.payment_service.exception;

public class ProviderNotFoundException extends RuntimeException{
    public ProviderNotFoundException(String providerName) {
        super("No provider found: " + providerName);
    }
}
