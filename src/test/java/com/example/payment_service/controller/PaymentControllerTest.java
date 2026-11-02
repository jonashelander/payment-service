package com.example.payment_service.controller;

import com.example.payment_service.Repository.TransactionRepository;
import com.example.payment_service.model.PaymentMethod;
import com.example.payment_service.model.Transaction;
import com.example.payment_service.model.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
    }

    @Test
    void postPayment_validRequest_savesTransactionToDatabase() throws Exception {
        String body = """
                {
                    "providerName": "Stripe",
                    "amount": 100.00,
                    "paymentMethod": "CARD"
                }
                """;

        mockMvc.perform(
                        post("/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionStatus").exists());

        assertEquals(1, transactionRepository.findAll().size());
    }

    @Test
    void getPaymentById_whenNotFound_returns404() throws Exception {
        Transaction existing = new Transaction();
        existing.setProviderName("Stripe");
        existing.setAmount(new BigDecimal("50"));
        existing.setPaymentMethod(PaymentMethod.CARD);
        existing.setTransactionStatus(TransactionStatus.COMPLETED);
        Transaction saved = transactionRepository.save(existing);

        mockMvc.perform(get("/payments/" + (saved.getId() + 1)))
                .andExpect(status().isNotFound());
    }
}
