package com.example.payment_service.dto;

import com.example.payment_service.model.PaymentMethod;

import java.math.BigDecimal;

public class PaymentRequest {
    private String providerName;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;

    public PaymentRequest() {
    }

    public String getProviderName() {
        return providerName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
