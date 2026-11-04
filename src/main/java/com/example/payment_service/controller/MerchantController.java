package com.example.payment_service.controller;

import com.example.payment_service.dto.MerchantRequest;
import com.example.payment_service.model.Merchant;
import com.example.payment_service.service.MerchantService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Merchant createMerchant(@RequestBody MerchantRequest request) {
        return merchantService.createMerchant(request.getName());
    }

    @GetMapping("/{id}")
    public Merchant getMerchantById(@PathVariable Long id) {
        return merchantService.findMerchantById(id);
    }
}
