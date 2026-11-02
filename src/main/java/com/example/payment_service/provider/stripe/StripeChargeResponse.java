package com.example.payment_service.provider.stripe;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StripeChargeResponse {
    @JsonProperty("id")
    private String pspTransactionId;
    private String status;
    private BigDecimal amount;
    private String currency;

    public String getPspTransactionId() {
        return pspTransactionId;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }
}
