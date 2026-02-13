package com.merdeleine.catalog.controller;


import com.merdeleine.catalog.dto.RefsResponse;
import com.merdeleine.catalog.service.InternalRefsService;
import com.merdeleine.catalog.service.SellWindowExpireService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal")
public class InternalRefsController {

    private final InternalRefsService refsService;
    private final SellWindowExpireService sellWindowExpireService;

    public InternalRefsController(InternalRefsService refsService, SellWindowExpireService sellWindowExpireService) {
        this.refsService = refsService;
        this.sellWindowExpireService = sellWindowExpireService;
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
}
