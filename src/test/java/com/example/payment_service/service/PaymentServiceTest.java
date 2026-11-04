package com.example.payment_service.service;

import com.example.payment_service.Repository.TransactionRepository;
import com.example.payment_service.exception.ProviderNotFoundException;
import com.example.payment_service.exception.TransactionNotFoundException;
import com.example.payment_service.model.Merchant;
import com.example.payment_service.model.PaymentMethod;
import com.example.payment_service.model.Transaction;
import com.example.payment_service.model.TransactionStatus;
import com.example.payment_service.provider.ChargeResult;
import com.example.payment_service.provider.PaymentProvider;
import com.example.payment_service.provider.ProviderRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    ProviderRegistry providerRegistry;

    @Mock
    TransactionRepository transactionRepository;

    @Mock
    PaymentProvider paymentProvider;

    @Mock
    MerchantService merchantService;

    @InjectMocks
    PaymentService paymentService;

    @Test
    void processPayment_whenChargeSucceeds_returnsCompletedTransaction() {
        Merchant merchant = new Merchant();
        merchant.setName("Acme Corp");

        when(providerRegistry.findByName("Stripe")).thenReturn(Optional.of(paymentProvider));
        when(merchantService.findMerchantByName("Acme Corp")).thenReturn(merchant);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentProvider.charge(any(), any(), any())).thenReturn(new ChargeResult("ch_abc123", "succeeded"));

        Transaction result = paymentService.processPayment("Stripe", new BigDecimal("100"), PaymentMethod.CARD, "pm_card_visa", "Acme Corp");

        assertEquals(TransactionStatus.COMPLETED, result.getTransactionStatus());
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void processPayment_whenChargeThrows_returnsFailedTransactionAndRethrows() {
        Merchant merchant = new Merchant();
        merchant.setName("Acme Corp");

        when(providerRegistry.findByName("Stripe")).thenReturn(Optional.of(paymentProvider));
        when(merchantService.findMerchantByName("Acme Corp")).thenReturn(merchant);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentProvider.charge(any(), any(), any())).thenThrow(new RuntimeException("Connection timed out"));

        assertThrows(RuntimeException.class, () -> paymentService.processPayment("Stripe", new BigDecimal(100), PaymentMethod.CARD, "pm_card_visa", "Acme Corp"));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void findTransactionById_whenNotFound_throwsTransactionNotFoundException() {
        when(transactionRepository.findById(anyLong())).thenReturn(Optional.empty());
        assertThrows(TransactionNotFoundException.class, () -> paymentService.findTransactionById(99L));
    }


}
