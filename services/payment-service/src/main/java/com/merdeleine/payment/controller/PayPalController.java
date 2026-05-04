package com.merdeleine.payment.controller;

import com.merdeleine.payment.dto.CaptureOrderResponse;
import com.merdeleine.payment.dto.CreateOrderRequest;
import com.merdeleine.payment.dto.CreateOrderResponse;
import com.merdeleine.payment.service.PayPalService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/paypal")

public class PayPalController {

    private final PayPalService payPalService;

    public PayPalController(PayPalService payPalService) {
        this.payPalService = payPalService;
    }

    @PostMapping("/orders")
    public CreateOrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return payPalService.createOrder(request);
    }

    @PostMapping("/orders/{orderId}/capture")
    public CaptureOrderResponse captureOrder(@PathVariable String orderId) {
        return payPalService.captureOrder(orderId);
    }
}