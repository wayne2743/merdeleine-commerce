package com.merdeleine.payment.service;

import com.merdeleine.enums.OrderStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderQueryService {

    private final RestClient restClient;

    public OrderQueryService(
            RestClient.Builder restClientBuilder,
            @Value("${services.order-service.base-url}") String orderServiceBaseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(orderServiceBaseUrl)
                .build();
    }

    public PaypalPaymentService.OrderSnapshot getPayableOrder(UUID orderId) {
        OrderResponse order = getOrder(orderId);

        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }

        if (order.status() != OrderStatus.PAYMENT_REQUESTED) {
            throw new IllegalStateException("Order is not payable, status=" + order.status());
        }

        if (order.totalAmountCents() == null || order.totalAmountCents() <= 0) {
            throw new IllegalStateException("Invalid order amount for payment: " + order.totalAmountCents());
        }

        if (order.currency() == null || order.currency().isBlank()) {
            throw new IllegalStateException("Invalid order currency for payment");
        }

        String description = "merdeleine order " + order.orderNo();
        return new PaypalPaymentService.OrderSnapshot(
                order.orderId(),
                order.totalAmountCents(),
                order.currency(),
                description
        );
    }

    public UUID getSellWindowIdByOrderId(UUID orderId) {
        OrderResponse order = getOrder(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
        return order.sellWindowId();
    }

    public Map<UUID, UUID> getSellWindowIdsByOrderIds(List<UUID> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Map.of();
        }

        List<OrderSellWindowRefResponse> refs = fetchOrderRefs(orderIds);
        Map<UUID, UUID> result = new LinkedHashMap<>();
        for (OrderSellWindowRefResponse ref : refs) {
            result.put(ref.orderId(), ref.sellWindowId());
        }
        return result;
    }

    public Map<UUID, UUID> getCustomerIdsByOrderIds(List<UUID> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Map.of();
        }

        List<OrderSellWindowRefResponse> refs = fetchOrderRefs(orderIds);
        Map<UUID, UUID> result = new LinkedHashMap<>();
        for (OrderSellWindowRefResponse ref : refs) {
            if (ref.customerId() != null) {
                result.put(ref.orderId(), ref.customerId());
            }
        }
        return result;
    }

    public OrderBatchResult batchGetOrderRefs(List<UUID> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return new OrderBatchResult(Map.of(), Map.of());
        }

        List<OrderSellWindowRefResponse> refs = fetchOrderRefs(orderIds);
        Map<UUID, UUID> sellWindowMap = new LinkedHashMap<>();
        Map<UUID, UUID> customerMap = new LinkedHashMap<>();
        for (OrderSellWindowRefResponse ref : refs) {
            if (ref.sellWindowId() != null) sellWindowMap.put(ref.orderId(), ref.sellWindowId());
            if (ref.customerId() != null) customerMap.put(ref.orderId(), ref.customerId());
        }
        return new OrderBatchResult(sellWindowMap, customerMap);
    }

    private List<OrderSellWindowRefResponse> fetchOrderRefs(List<UUID> orderIds) {
        List<OrderSellWindowRefResponse> refs = restClient.post()
                .uri("/order/internal/orders/_batch-sell-window")
                .body(new BatchOrderSellWindowRequest(orderIds))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new RuntimeException("order-service /order/internal/orders/_batch-sell-window failed: " + res.getStatusCode());
                })
                .body(new ParameterizedTypeReference<List<OrderSellWindowRefResponse>>() {});

        return refs == null ? Collections.emptyList() : refs;
    }

    public record OrderBatchResult(Map<UUID, UUID> orderSellWindowMap, Map<UUID, UUID> orderCustomerMap) {
    }

    private OrderResponse getOrder(UUID orderId) {
        return restClient.get()
                .uri("/orders/{orderId}", orderId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new RuntimeException("order-service /orders/{orderId} failed: " + res.getStatusCode());
                })
                .body(OrderResponse.class);
    }

    private record OrderResponse(
            UUID orderId,
            String orderNo,
            OrderStatus status,
            Integer totalAmountCents,
            String currency,
            UUID sellWindowId
    ) {
    }

    private record BatchOrderSellWindowRequest(List<UUID> orderIds) {
    }

    private record OrderSellWindowRefResponse(UUID orderId, UUID sellWindowId, UUID customerId) {
    }
}