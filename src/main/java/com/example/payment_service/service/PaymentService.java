package com.example.payment_service.service;

import com.example.payment_service.Repository.TransactionRepository;
import com.example.payment_service.exception.ProviderNotFoundException;
import com.example.payment_service.exception.TransactionNotFoundException;
import com.example.payment_service.model.PaymentMethod;
import com.example.payment_service.model.Transaction;
import com.example.payment_service.model.TransactionStatus;
import com.example.payment_service.provider.PaymentProvider;
import com.example.payment_service.provider.ProviderRegistry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PaymentService {
    private final ProviderRegistry providerRegistry;
    private final TransactionRepository transactionRepository;

    public PaymentService(ProviderRegistry providerRegistry, TransactionRepository transactionRepository) {
        this.providerRegistry = providerRegistry;
        this.transactionRepository = transactionRepository;
    }

    public Transaction processPayment(String providerName, BigDecimal amount, PaymentMethod method) {
        PaymentProvider provider = providerRegistry.findByName(providerName).orElseThrow(() -> new ProviderNotFoundException(providerName));
        Transaction transaction = new Transaction();
        transaction.setProviderName(providerName);
        transaction.setAmount(amount);
        transaction.setPaymentMethod(method);
        transaction.setTransactionStatus(TransactionStatus.PENDING);
        transactionRepository.save(transaction);

        try {
            provider.charge(amount, method);
            transaction.setTransactionStatus(TransactionStatus.COMPLETED);
            transactionRepository.save(transaction);
        } catch (RuntimeException e) {
            transaction.setTransactionStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw e;
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
