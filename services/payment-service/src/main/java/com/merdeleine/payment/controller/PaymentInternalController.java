package com.merdeleine.payment.controller;

import com.merdeleine.payment.service.PaymentExpiryService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/payments")
public class PaymentInternalController {

    private final PaymentExpiryService paymentExpiryService;

    public PaymentInternalController(PaymentExpiryService paymentExpiryService) {
        this.paymentExpiryService = paymentExpiryService;
    }

    @PostMapping("/expire-due")
    public Map<String, Object> expireDue(@RequestParam(defaultValue = "200") int batchSize) {
        int n = paymentExpiryService.expireDuePayments(batchSize);
        return Map.of("expiredCount", n);
    }
}