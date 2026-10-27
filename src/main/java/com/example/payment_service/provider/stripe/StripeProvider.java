package com.example.payment_service.provider.stripe;

import com.example.payment_service.config.ProviderProperties;
import com.example.payment_service.model.PaymentMethod;
import com.example.payment_service.provider.AbstractPaymentProvider;
import jakarta.annotation.PostConstruct;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;


import java.math.BigDecimal;

@Component
public class StripeProvider extends AbstractPaymentProvider {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ProviderProperties providerProperties;

    public StripeProvider(RestTemplate restTemplate, ObjectMapper objectMapper, ProviderProperties providerProperties) {
        this.apiUrl = providerProperties.getStripe().getBaseUrl();
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.providerProperties = providerProperties;
    }

    @Override
    public String getName() {
        return "Stripe";
    }

    @Retryable(retryFor = RuntimeException.class, maxAttempts = 3)
    @Override
    public String charge(BigDecimal amount, PaymentMethod method) {
        String responseBody = restTemplate.postForObject(apiUrl, amount, String.class);
        StripeChargeResponse response = objectMapper.readValue(responseBody, StripeChargeResponse.class);
        return "Stripe charge " + response.getPspTransactionId() + " status: " + response.getStatus();
    }

    @PostConstruct
    private void logConfig() {
        String apiKey = providerProperties.getStripe().getApiKey();
        String maskedApiKey = apiKey.substring(0,8) + "***";
        System.out.println("Stripe masked apiKey " + maskedApiKey);
    }

    @Recover
    public String recover(RuntimeException e, BigDecimal amount, PaymentMethod method){
        throw new RuntimeException("Stripe charge failed after retries: " + e.getMessage());
    }

}
