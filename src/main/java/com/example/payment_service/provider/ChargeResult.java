package com.example.payment_service.provider;

public class ChargeResult {
    private final String providerTransactionId;
    private final String status;

    public ChargeResult(String providerTransactionId, String status) {
        this.providerTransactionId = providerTransactionId;
        this.status = status;
    }

    public String getProviderTransactionId() {
        return providerTransactionId;
    }

    public String getStatus() {
        return status;
    }
}
