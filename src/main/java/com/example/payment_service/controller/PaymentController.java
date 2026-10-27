package com.example.payment_service.controller;

import com.example.payment_service.dto.PaymentRequest;
import com.example.payment_service.model.Transaction;
import com.example.payment_service.model.TransactionStatus;
import com.example.payment_service.service.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentServiceTest;

    public PaymentController(PaymentService paymentServiceTest) {
        this.paymentServiceTest = paymentServiceTest;
    }

    @PostMapping
    public Transaction processPayment(@RequestBody PaymentRequest request) {
        return paymentServiceTest.processPayment(request.getProviderName(), request.getAmount(), request.getPaymentMethod());
    }

    @GetMapping("/{id}")
    public Transaction getTransactionById(@PathVariable Long id) {
        return paymentServiceTest.findTransactionById(id);
    }

    @GetMapping()
    public List<Transaction> getTransactionsByStatus(@RequestParam TransactionStatus status){
        return paymentServiceTest.findTransactionsByStatus(status);
    }
}
