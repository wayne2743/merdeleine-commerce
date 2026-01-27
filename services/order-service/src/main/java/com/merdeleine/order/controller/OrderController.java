package com.merdeleine.order.controller;

import com.merdeleine.order.dto.*;
import com.merdeleine.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@RequestBody @Valid OrderCreateRequest req) {
        OrderResponse res = orderService.create(req);
        return ResponseEntity
                .created(URI.create("/api/orders/" + res.id()))
                .body(res);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id) {
        return orderService.get(id);
    }

    @GetMapping
    public List<OrderResponse> list() {
        return orderService.list();
    }

    @PutMapping("/{id}")
    public OrderResponse update(@PathVariable UUID id, @RequestBody @Valid OrderUpdateRequest req) {
        return orderService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
