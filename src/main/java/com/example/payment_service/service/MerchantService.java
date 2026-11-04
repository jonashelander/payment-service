package com.example.payment_service.service;

import com.example.payment_service.Repository.MerchantRepository;
import com.example.payment_service.exception.DuplicateMerchantException;
import com.example.payment_service.exception.MerchantNotFoundException;
import com.example.payment_service.model.Merchant;
import org.springframework.stereotype.Service;

@Service
public class MerchantService {

    private final MerchantRepository merchantRepository;

    public MerchantService(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    public Merchant createMerchant(String name) {
        if (merchantRepository.findByname(name).isPresent()) {
            throw new DuplicateMerchantException(name);
        }
        Merchant merchant = new Merchant();
        merchant.setName(name);
        return merchantRepository.save(merchant);
    }

    public Merchant findMerchantById(Long id) {
        return merchantRepository.findById(id).orElseThrow(() -> new MerchantNotFoundException(id));
    }

    public Merchant findMerchantByName(String name) {
        return merchantRepository.findByname(name).orElseThrow(() -> new MerchantNotFoundException(name));
    }
}
