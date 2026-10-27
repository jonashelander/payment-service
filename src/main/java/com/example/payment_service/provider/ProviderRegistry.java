package com.example.payment_service.provider;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ProviderRegistry {
    private final List<PaymentProvider> providers;

    public ProviderRegistry(List<PaymentProvider> providers) {
        this.providers = providers;
    }

   public Optional<PaymentProvider> findByName(String name) {
       return providers.stream().filter(p -> p.getName().equals(name)).findFirst();
    }
}

