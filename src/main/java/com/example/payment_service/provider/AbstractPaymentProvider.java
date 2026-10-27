package com.example.payment_service.provider;

import com.example.payment_service.model.PaymentMethod;

import java.math.BigDecimal;

public abstract class AbstractPaymentProvider implements PaymentProvider {
    protected String apiUrl;

    protected String buildLogMsg(String action, BigDecimal amount) {
        return action + " for " + amount;
    }
}