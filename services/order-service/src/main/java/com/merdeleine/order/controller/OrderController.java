package com.merdeleine.order.controller;

import com.merdeleine.enums.OrderStatus;
import com.merdeleine.order.dto.AutoReserveOrderDtos;
import com.merdeleine.order.dto.CreateOrderRequest;
import com.merdeleine.order.dto.OrderResponse;
import com.merdeleine.order.dto.UpdateOrderRequest;
import com.merdeleine.order.exception.BadRequestException;
import com.merdeleine.order.service.AutoReserveOrderService;
import com.merdeleine.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final AutoReserveOrderService service;

    public OrderController(OrderService orderService, AutoReserveOrderService service) {
        this.orderService = orderService;
        this.service = service;
    }

    @PostMapping
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.create(request);
    }

    @GetMapping("/{orderId}")
    public OrderResponse get(@PathVariable UUID orderId) {
        return orderService.get(orderId);
    }

    @GetMapping
    public List<OrderResponse> list(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID sellWindowId,
            @RequestParam(required = false) OrderStatus status
    ) {
        if (customerId != null && sellWindowId != null) {
            throw new BadRequestException("Provide either customerId or sellWindowId, not both");
        }
        if (customerId == null && sellWindowId == null) {
            throw new BadRequestException("Either customerId or sellWindowId is required");
        }
        if (customerId != null) {
            return orderService.listByCustomerId(customerId, status);
        }
        return orderService.listBySellWindowId(sellWindowId, status);
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

    @PostMapping("/auto-reserve")
    public AutoReserveOrderDtos.Response autoReserve(
            @Valid @RequestBody AutoReserveOrderDtos.Request req,
            @RequestHeader(value = "X-USER-ID", required = false) String userId
    ) {
        // 如果 header 中没有 userId，从 request 中获取
        // 否则使用 header 中的 userId 覆盖 request 中的 customerId
        if (userId != null && !userId.isEmpty()) {
            AutoReserveOrderDtos.Request newReq = new AutoReserveOrderDtos.Request(
                    req.sellWindowId(),
                    req.productId(),
                    req.qty(),
                    req.unitPriceCents(),
                    req.currency(),
                    UUID.fromString(userId),
                    req.delivery(),
                    req.shippingAddress()
            );
            return service.autoReserve(newReq);
        }
        return service.autoReserve(req);
    }
}
