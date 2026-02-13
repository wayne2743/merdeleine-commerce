package com.merdeleine.order.controller;


import com.merdeleine.order.dto.CloseQuotaDtos;
import com.merdeleine.order.service.SellWindowQuotaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/sell-window-quotas")
public class SellWindowQuotaInternalController {

    private final SellWindowQuotaService service;

    public SellWindowQuotaInternalController(SellWindowQuotaService service) {
        this.service = service;
    }

    @PostMapping("/close")
    @ResponseStatus(HttpStatus.OK)
    public CloseQuotaDtos.CloseQuotaResponse close(@Valid @RequestBody CloseQuotaDtos.CloseQuotaRequest req) {
        return service.close(req);
    }
}
