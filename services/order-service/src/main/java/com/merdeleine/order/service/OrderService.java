package com.merdeleine.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.enums.OrderStatus;
import com.merdeleine.order.dto.CreateOrderRequest;
import com.merdeleine.order.dto.OrderResponse;
import com.merdeleine.order.dto.UpdateOrderRequest;
import com.merdeleine.order.entity.Order;
import com.merdeleine.order.entity.OutboxEvent;
import com.merdeleine.order.enums.OutboxEventStatus;
import com.merdeleine.order.mapper.OrderEventMapper;
import com.merdeleine.order.mapper.OrderMapper;
import com.merdeleine.order.repository.OrderRepository;
import com.merdeleine.order.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final QuotaService quotaService;
    private final String orderReservedTopic;
    private final String orderCancelledTopic;

    public OrderService(OrderRepository orderRepository,
                        OutboxEventRepository outboxEventRepository,
                        ObjectMapper objectMapper,
                        QuotaService quotaService,
                        @Value("${app.kafka.topic.order-reserved-events}") String orderReservedTopic,
                        @Value("${app.kafka.topic.order-cancelled-events}") String orderCancelledTopic) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.quotaService = quotaService;
        this.orderReservedTopic = orderReservedTopic;
        this.orderCancelledTopic = orderCancelledTopic;

    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {

        int qty            = request.quantity();
        int unitPrice      = request.unitPriceCents();
        int subtotalDelta  = qty * unitPrice;

        // 1) 先原子扣額度（防超賣）
        quotaService.reserveOrThrow(
                request.sellWindowId(),
                request.productId(),
                qty
        );

        // 2) 判斷是否已有同一 sell window 的既有 RESERVED 訂單
        Order order = orderRepository
                .findActiveOrderByCustomerAndSellWindow(
                        request.customerId(), request.sellWindowId(), OrderStatus.RESERVED)
                .map(existing -> {
                    // --- 合併：把新增數量疊加到既有 order item ---
                    var item = existing.getItem();
                    item.setQuantity(item.getQuantity() + qty);
                    item.setSubtotalCents(item.getSubtotalCents() + subtotalDelta);
                    existing.setTotalAmountCents(existing.getTotalAmountCents() + subtotalDelta);
                    return existing;
                })
                .orElseGet(() -> {
                    // --- 建新單 ---
                    Order newOrder = OrderMapper.toEntity(request, OrderStatus.RESERVED);
                    return orderRepository.save(newOrder);
                });

        // 3) 不論新建或合併，都只發本次 delta qty 給 threshold
        writeOutbox(
                "Order",
                order.getId(),
                orderReservedTopic,
                new OrderEventMapper().toOrderEvent(order, orderReservedTopic, qty)
        );

        return OrderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse get(UUID orderId) {
        Order order = findOrder(orderId);
        return OrderMapper.toResponse(order);
    }

    @Transactional
    public List<OrderResponse> listByCustomerId(UUID customerId, OrderStatus status) {
        List<Order> orders = status == null
                ? orderRepository.findByCustomerIdAndStatusNot(customerId, OrderStatus.CANCELLED)
                : orderRepository.findByCustomerIdAndStatus(customerId, status);

        return orders.stream()
                .map(OrderMapper::toResponse)
                .toList();
    }

    @Transactional
    public List<OrderResponse> listBySellWindowId(UUID sellWindowId, OrderStatus status) {
        List<Order> orders = status == null
                ? orderRepository.findBySellWindowIdAndStatusNot(sellWindowId, OrderStatus.CANCELLED)
                : orderRepository.findBySellWindowIdAndStatus(sellWindowId, status);

        return orders.stream()
                .map(OrderMapper::toResponse)
                .toList();
    }

    @Transactional
    public List<OrderSellWindowRef> batchGetSellWindowRefs(List<UUID> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return List.of();
        }

        List<UUID> uniqueOrderIds = new LinkedHashSet<>(orderIds).stream().toList();
        Map<UUID, Order> orderMap = orderRepository.findAllById(uniqueOrderIds).stream()
                .collect(Collectors.toMap(Order::getId, o -> o, (a, b) -> a));

        return uniqueOrderIds.stream()
                .map(orderId -> {
                    Order o = orderMap.get(orderId);
                    return o == null
                            ? new OrderSellWindowRef(orderId, null, null)
                            : new OrderSellWindowRef(orderId, o.getSellWindowId(), o.getCustomerId());
                })
                .toList();
    }

    @Transactional
    public OrderResponse update(UUID orderId, UpdateOrderRequest req) {
        Order order = findOrder(orderId);

        if (order.getStatus() != OrderStatus.RESERVED) {
            throw new IllegalStateException("Only RESERVED orders can update quantity");
        }

        var item = order.getItem();
        if (item == null) {
            throw new IllegalStateException("Order item not found: " + orderId);
        }

        int oldQty = item.getQuantity();
        int newQty = req.quantity();
        int deltaQty = newQty - oldQty;

        if (deltaQty > 0) {
            quotaService.reserveOrThrow(order.getSellWindowId(), item.getProductId(), deltaQty);
        } else if (deltaQty < 0) {
            quotaService.release(order.getSellWindowId(), item.getProductId(), -deltaQty);
        }

        int unitPrice = item.getUnitPriceCents();
        int newSubtotal = newQty * unitPrice;

        item.setQuantity(newQty);
        item.setSubtotalCents(newSubtotal);
        order.setTotalAmountCents(newSubtotal);

        if (deltaQty > 0) {
            writeOutbox(
                    "Order",
                    order.getId(),
                    orderReservedTopic,
                    new OrderEventMapper().toOrderEvent(order, orderReservedTopic, deltaQty)
            );
        } else if (deltaQty < 0) {
            writeOutbox(
                    "Order",
                    order.getId(),
                    orderCancelledTopic,
                    new OrderEventMapper().toOrderCancelledEvent(order, orderCancelledTopic, -deltaQty)
            );
        }

        return OrderMapper.toResponse(order);
    }

    @Transactional
    public void cancel(UUID orderId) {
        Order order = findOrder(orderId);

        // Idempotent guard: already cancelled orders should not release quota twice.
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }

        var item = order.getItem();
        order.setStatus(OrderStatus.CANCELLED);
        // Emit reverse event before clearing item so payload still has product/qty for threshold rollback.
        if (item != null && order.getSellWindowId() != null) {
            writeOutbox(
                    "Order",
                    order.getId(),
                    orderCancelledTopic,
                    new OrderEventMapper().toOrderCancelledEvent(order, orderCancelledTopic)
            );

            // Release quota so /api/catalog/views/product-sell-windows reflects the cancellation.
            quotaService.release(order.getSellWindowId(), item.getProductId(), item.getQuantity());
        }
        order.clearItem(); // orphanRemoval = true
    }

    private Order findOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }

    private void writeOutbox(String aggregateType, UUID aggregateId, String eventType, Object payloadObj) {
        try {
            OutboxEvent evt = new OutboxEvent();
            evt.setId(UUID.randomUUID());
            evt.setAggregateType(aggregateType);
            evt.setAggregateId(aggregateId);
            evt.setEventType(eventType);
            evt.setPayload(objectMapper.valueToTree(payloadObj));
            evt.setStatus(OutboxEventStatus.NEW);
            outboxEventRepository.save(evt);
        } catch (Exception e) {
            // 讓 transaction rollback，確保「業務寫入 + outbox」同生共死
            throw new RuntimeException("Failed to write outbox event", e);
        }
    }

    public record OrderSellWindowRef(UUID orderId, UUID sellWindowId, UUID customerId) {
    }
}
