package com.merdeleine.catalog.controller;

import com.merdeleine.catalog.dto.AutoGroupOrderDtos;
import com.merdeleine.catalog.service.AutoGroupOrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog/customer")
public class CustomerAutoGroupOrderController {

    private final AutoGroupOrderService service;

    public CustomerAutoGroupOrderController(AutoGroupOrderService service) {
        this.service = service;
    }

    @PostMapping("/auto-group-orders")
    public AutoGroupOrderDtos.Response autoGroupOrders(@Valid @RequestBody AutoGroupOrderDtos.Request req) {
        return service.autoGroupOrder(req);
    }
}