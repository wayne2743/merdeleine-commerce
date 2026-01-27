package com.merdeleine.order.controller;

import com.merdeleine.order.dto.CreateOrderRequest;
import com.merdeleine.order.dto.OrderResponse;
import com.merdeleine.order.dto.UpdateOrderRequest;
import com.merdeleine.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.create(request);
    }

    @GetMapping("/{orderId}")
    public OrderResponse get(@PathVariable UUID orderId) {
        return orderService.get(orderId);
    }

    @PutMapping("/{orderId}")
    public OrderResponse update(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderRequest request
    ) {
        return orderService.update(orderId, request);
    }

    @DeleteMapping("/{orderId}")
    public void cancel(@PathVariable UUID orderId) {
        orderService.cancel(orderId);
    }
}
