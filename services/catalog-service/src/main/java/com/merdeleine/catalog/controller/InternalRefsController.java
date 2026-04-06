package com.merdeleine.catalog.controller;


import com.merdeleine.catalog.dto.OpenPaymentRequest;
import com.merdeleine.catalog.dto.OpenPaymentResponse;
import com.merdeleine.catalog.dto.RefsResponse;
import com.merdeleine.catalog.service.InternalRefsService;
import com.merdeleine.catalog.service.SellWindowExpireService;
import com.merdeleine.catalog.service.SellWindowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal")
public class InternalRefsController {

    private final InternalRefsService refsService;
    private final SellWindowExpireService sellWindowExpireService;
    private final SellWindowService sellWindowService;

    public InternalRefsController(InternalRefsService refsService,
                                  SellWindowExpireService sellWindowExpireService,
                                  SellWindowService sellWindowService) {
        this.refsService = refsService;
        this.sellWindowExpireService = sellWindowExpireService;
        this.sellWindowService = sellWindowService;
    }

    @GetMapping("/refs")
    public ResponseEntity<RefsResponse> getRefs(
            @RequestParam UUID productId,
            @RequestParam UUID sellWindowId
    ) {
        try {
            return ResponseEntity.ok(refsService.getRefs(productId, sellWindowId));
        } catch (InternalRefsService.NotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/sell-windows/close-expired")
    public SellWindowExpireService.CloseExpiredResult closeExpired(
            @RequestParam(name = "limit", defaultValue = "200") int limit
    ) {
        return sellWindowExpireService.closeExpired(limit);
    }

    @PostMapping("/eventbridge/sell-windows/close-overdue")
    public SellWindowExpireService.CloseExpiredResult closeOverdueForEventBridge(
            @RequestParam(name = "limit", defaultValue = "200") int limit
    ) {
        return sellWindowExpireService.closeExpired(limit);
    }

    @PostMapping("/sell-windows/{sellWindowId}/open-payment")
    public ResponseEntity<OpenPaymentResponse> openPayment(
            @PathVariable UUID sellWindowId,
            @RequestBody(required = false) OpenPaymentRequest req
    ) {
        OpenPaymentResponse resp = sellWindowService.openPayment(sellWindowId, req);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/sell-windows/_batch-names")
    public List<SellWindowService.SellWindowNameRef> batchGetSellWindowNames(
            @RequestBody(required = false) BatchSellWindowNameRequest request
    ) {
        List<UUID> sellWindowIds = request == null ? List.of() : request.sellWindowIds();
        return sellWindowService.batchGetNames(sellWindowIds);
    }

    public record BatchSellWindowNameRequest(List<UUID> sellWindowIds) {
    }
}
