package com.merdeleine.payment.controller;

import com.merdeleine.payment.dto.CapturePaypalPaymentResponse;
import com.merdeleine.payment.dto.CreatePaypalPaymentRequest;
import com.merdeleine.payment.dto.CreatePaypalPaymentResponse;
import com.merdeleine.payment.service.PaypalPaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/paypal")
public class PaypalPaymentController {

    private final PaypalPaymentService paypalPaymentService;

    public PaypalPaymentController(PaypalPaymentService paypalPaymentService) {
        this.paypalPaymentService = paypalPaymentService;
    }

    @PostMapping("/create")
    public CreatePaypalPaymentResponse create(@Valid @RequestBody CreatePaypalPaymentRequest request) {
        return paypalPaymentService.createPayment(request);
    }

    @PostMapping("/capture/{paypalOrderId}")
    public CapturePaypalPaymentResponse capture(@PathVariable String paypalOrderId) {
        return paypalPaymentService.capturePayment(paypalOrderId);
    }
}