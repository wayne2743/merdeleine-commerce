package com.merdeleine.order.controller;

import com.merdeleine.order.dto.StorePickupLocationResponse;
import com.merdeleine.order.service.StorePickupLocationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/store-pickup-locations")
public class StorePickupLocationController {

    private final StorePickupLocationService service;

    public StorePickupLocationController(StorePickupLocationService service) {
        this.service = service;
    }

    @GetMapping
    public List<StorePickupLocationResponse> listActive() {
        return service.listActive();
    }
}

