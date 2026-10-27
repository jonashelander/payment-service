package com.example.payment_service.Repository;

import com.example.payment_service.model.Transaction;
import com.example.payment_service.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByTransactionStatus(TransactionStatus status);
}
