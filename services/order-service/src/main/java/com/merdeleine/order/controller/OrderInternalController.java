package com.merdeleine.order.controller;

import com.merdeleine.order.service.OrderService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/order/internal/orders")
public class OrderInternalController {

    private final OrderService orderService;

    public OrderInternalController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/_batch-sell-window")
    public List<OrderService.OrderSellWindowRef> batchGetSellWindowRefs(@RequestBody BatchRequest request) {
        List<UUID> orderIds = request == null ? List.of() : request.orderIds();
        return orderService.batchGetSellWindowRefs(orderIds);
    }

    public record BatchRequest(List<UUID> orderIds) {
    }
}

