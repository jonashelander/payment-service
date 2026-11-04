package com.example.payment_service.dto;

import com.example.payment_service.model.PaymentMethod;

import java.math.BigDecimal;

public class PaymentRequest {
    private String providerName;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String cardToken;
    private String merchant;

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

    public String getCardToken() {
        return cardToken;
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

    public void setCardToken(String cardToken) {
        this.cardToken = cardToken;
    }

    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }
}
