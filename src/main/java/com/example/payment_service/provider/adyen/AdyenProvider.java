package com.example.payment_service.provider.adyen;

import com.example.payment_service.config.ProviderProperties;
import com.example.payment_service.model.PaymentMethod;
import com.example.payment_service.provider.AbstractPaymentProvider;
import com.example.payment_service.provider.ChargeResult;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import tools.jackson.dataformat.xml.XmlMapper;

import java.math.BigDecimal;

@Component
public class AdyenProvider extends AbstractPaymentProvider {
    private final RestTemplate restTemplate;
    private final ProviderProperties providerProperties;

    public AdyenProvider(RestTemplate restTemplate, ProviderProperties providerProperties) {
        this.apiUrl = providerProperties.getAdyen().getBaseUrl();
        this.restTemplate = restTemplate;
        this.providerProperties = providerProperties;
    }

    @Override
    public String getName() {
        return "Adyen";
    }

    @Override
    public ChargeResult charge(BigDecimal amount, PaymentMethod method, String cardToken) {
        String responseBody = restTemplate.postForObject(apiUrl, amount, String.class);
        XmlMapper xmlMapper = new XmlMapper();
        AdyenChargeResponse response = xmlMapper.readValue(responseBody, AdyenChargeResponse.class);
        return new ChargeResult(response.getPspTransactionId(), response.getStatus());
    }

    @PostConstruct
    private void logConfig() {
        String apiKey = providerProperties.getAdyen().getApiKey();
        String maskedApiKey = apiKey.substring(0,8) + "***";
        System.out.println("Adyen masked apiKey " + maskedApiKey);
    }
}
