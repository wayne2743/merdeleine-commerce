package com.merdeleine.catalog.controller;


import com.merdeleine.catalog.dto.OpenPaymentRequest;
import com.merdeleine.catalog.dto.OpenPaymentResponse;
import com.merdeleine.catalog.service.SellWindowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/sell-windows")
public class SellWindowInternalController {

    private final SellWindowService sellWindowService;

    public SellWindowInternalController(SellWindowService sellWindowService) {
        this.sellWindowService = sellWindowService;
    }


    @PostMapping("/{sellWindowId}/open-payment")
    public ResponseEntity<OpenPaymentResponse> openPayment(
            @PathVariable UUID sellWindowId,
            @RequestBody(required = false) OpenPaymentRequest req
    ) {
        OpenPaymentResponse resp = sellWindowService.openPayment(sellWindowId, req);
        return ResponseEntity.ok(resp);
    }
}