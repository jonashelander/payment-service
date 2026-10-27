package com.example.payment_service.provider.stripe;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StripeChargeResponse {
    private String pspTransactionId;
    private String status;
    private BigDecimal amount;

    public String getPspTransactionId() {
        return pspTransactionId;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
