package com.merdeleine.catalog.controller;

import com.merdeleine.catalog.dto.AutoGroupOrderDtos;
import com.merdeleine.catalog.service.AutoGroupOrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/customer")
public class CustomerAutoGroupOrderController {

    private final AutoGroupOrderService service;

    public CustomerAutoGroupOrderController(AutoGroupOrderService service) {
        this.service = service;
    }

    @PostMapping("/auto-group-orders")
    public AutoGroupOrderDtos.Response autoGroupOrders(
            @Valid @RequestBody AutoGroupOrderDtos.Request req,
            @RequestHeader(value = "X-USER-ID", required = false) String userId
    ) {
        // 如果 header 中有 userId，使用它來覆蓋或補充 request 中的 customerId
        if (userId != null && !userId.isEmpty()) {
            AutoGroupOrderDtos.Request newReq = new AutoGroupOrderDtos.Request(
                    req.productId(),
                    req.qty(),
                    UUID.fromString(userId),
                    req.predictedGroupOpenAt(),
                    req.predictedGroupEndAt(),
                    req.leadsDay(),
                    req.shipDay()
            );
            return service.autoGroupOrder(newReq);
        }
        return service.autoGroupOrder(req);
    }
}