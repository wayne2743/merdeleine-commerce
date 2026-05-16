package com.merdeleine.order.controller;

import com.merdeleine.order.dto.StorePickupLocationRequest;
import com.merdeleine.order.dto.StorePickupLocationResponse;
import com.merdeleine.order.service.StorePickupLocationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/store-pickup-locations")
public class StorePickupLocationAdminController {

    private final StorePickupLocationService service;

    public StorePickupLocationAdminController(StorePickupLocationService service) {
        this.service = service;
    }

    @PostMapping
    public StorePickupLocationResponse create(@Valid @RequestBody StorePickupLocationRequest req) {
        return service.create(req);
    }

    @GetMapping
    public List<StorePickupLocationResponse> listAll() {
        return service.listAll();
    }

    @GetMapping("/{id}")
    public StorePickupLocationResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public StorePickupLocationResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody StorePickupLocationRequest req
    ) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}

