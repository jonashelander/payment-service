package com.example.payment_service.provider;

import com.example.payment_service.model.PaymentMethod;

import java.math.BigDecimal;

public interface PaymentProvider {
    String getName();
    String charge(BigDecimal amount, PaymentMethod method);
}