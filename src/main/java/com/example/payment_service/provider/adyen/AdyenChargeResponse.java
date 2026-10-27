package com.example.payment_service.provider.adyen;


import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.math.BigDecimal;

@JacksonXmlRootElement(localName = "response")
public class AdyenChargeResponse {
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
