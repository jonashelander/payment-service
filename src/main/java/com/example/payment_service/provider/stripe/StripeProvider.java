package com.example.payment_service.provider.stripe;

import com.example.payment_service.config.ProviderProperties;
import com.example.payment_service.model.PaymentMethod;
import com.example.payment_service.provider.AbstractPaymentProvider;
import com.example.payment_service.provider.ChargeResult;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
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

    @Retryable(retryFor = {HttpServerErrorException.class, ResourceAccessException.class}, maxAttempts = 3)
    @Override
    public ChargeResult charge(BigDecimal amount, PaymentMethod method, String cardToken) {
        long amountInMinorUnits = toMinorUnits(amount);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(providerProperties.getStripe().getApiKey());
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("amount", String.valueOf(amountInMinorUnits));
        body.add("currency", "eur");
        body.add("payment_method", cardToken);
        body.add("confirm", "true");
        // No frontend exists to redirect the customer to and back — never allow
        // redirect-based payment methods (e.g. bank redirects), only direct card charges.
        body.add("automatic_payment_methods[enabled]", "true");
        body.add("automatic_payment_methods[allow_redirects]", "never");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        String responseBody = restTemplate.postForObject(apiUrl + "/v1/payment_intents", requestEntity, String.class);
        StripeChargeResponse response = objectMapper.readValue(responseBody, StripeChargeResponse.class);

        return new ChargeResult(response.getPspTransactionId(), response.getStatus());
    }

    // EUR is a two-decimal currency, so Stripe expects the amount in cents: multiply by 100.
    // longValueExact() fails loudly instead of silently rounding if amount has more than 2 decimal places.
    private long toMinorUnits(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).longValueExact();
    }

    @PostConstruct
    private void logConfig() {
        String apiKey = providerProperties.getStripe().getApiKey();
        String maskedApiKey = apiKey.substring(0,8) + "***";
        System.out.println("Stripe masked apiKey " + maskedApiKey);
    }

    @Recover
    public ChargeResult recover(RuntimeException e, BigDecimal amount, PaymentMethod method, String cardToken){
        throw new RuntimeException("Stripe charge failed: " + e.getMessage());
    }

}
