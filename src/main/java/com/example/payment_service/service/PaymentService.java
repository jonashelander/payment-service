package com.example.payment_service.service;

import com.example.payment_service.Repository.TransactionRepository;
import com.example.payment_service.exception.ChargeFailedException;
import com.example.payment_service.exception.ProviderNotFoundException;
import com.example.payment_service.exception.TransactionNotFoundException;
import com.example.payment_service.model.Merchant;
import com.example.payment_service.model.PaymentMethod;
import com.example.payment_service.model.Transaction;
import com.example.payment_service.model.TransactionStatus;
import com.example.payment_service.provider.ChargeResult;
import com.example.payment_service.provider.PaymentProvider;
import com.example.payment_service.provider.ProviderRegistry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PaymentService {
    private final ProviderRegistry providerRegistry;
    private final TransactionRepository transactionRepository;
    private final MerchantService merchantService;

    public PaymentService(ProviderRegistry providerRegistry, TransactionRepository transactionRepository, MerchantService merchantService) {
        this.providerRegistry = providerRegistry;
        this.transactionRepository = transactionRepository;
        this.merchantService = merchantService;
    }

    public Transaction  processPayment(String providerName, BigDecimal amount, PaymentMethod method, String cardToken, String merchantName) {
        PaymentProvider provider = providerRegistry.findByName(providerName).orElseThrow(() -> new ProviderNotFoundException(providerName));
        Merchant merchant = merchantService.findMerchantByName(merchantName);
        Transaction transaction = new Transaction();
        transaction.setProviderName(providerName);
        transaction.setAmount(amount);
        transaction.setPaymentMethod(method);
        transaction.setTransactionStatus(TransactionStatus.PENDING);
        transaction.setMerchant(merchant);
        transactionRepository.save(transaction);

        try {
            ChargeResult result = provider.charge(amount, method, cardToken);
            transaction.setPspTransactionStatus(result.getStatus());
            transaction.setTransactionStatus(
                "succeeded".equals(result.getStatus()) ? TransactionStatus.COMPLETED : TransactionStatus.FAILED
            );
            transactionRepository.save(transaction);
        } catch (RuntimeException e) {
            transaction.setTransactionStatus(TransactionStatus.FAILED);
            transaction.setPspTransactionStatus(e.getMessage());
            transactionRepository.save(transaction);
            throw new ChargeFailedException(transaction, e);
        }
        return transaction;
    }

    public Transaction findTransactionById(Long id) {
        return transactionRepository.findById(id).orElseThrow(() -> new TransactionNotFoundException(id));
    }

    public List<Transaction> findTransactionsByStatus(TransactionStatus status) {
        return transactionRepository.findByTransactionStatus(status);
    }
}
