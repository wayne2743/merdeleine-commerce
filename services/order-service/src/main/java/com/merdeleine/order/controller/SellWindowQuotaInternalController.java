package com.merdeleine.order.controller;


import com.merdeleine.order.dto.CloseQuotaDtos;
import com.merdeleine.order.dto.SellWindowQuotaBatchDto;
import com.merdeleine.order.dto.SellWindowQuotaUpsertDtos;
import com.merdeleine.order.service.SellWindowQuotaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order/internal/sell-window-quotas")
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

    @PostMapping("/_batch")
    public List<SellWindowQuotaBatchDto.QuotaResponse> batchGet(
            @RequestBody SellWindowQuotaBatchDto.BatchRequest req
    ) {
        List<SellWindowQuotaBatchDto.Key> keys =
                (req == null) ? List.of() : req.keys();

        return service.batchGet(keys);
    }

    @PutMapping("/upsert")
    public SellWindowQuotaUpsertDtos.Response upsert(@Valid @RequestBody SellWindowQuotaUpsertDtos.Request req) {
        return service.upsert(req);
    }

}
