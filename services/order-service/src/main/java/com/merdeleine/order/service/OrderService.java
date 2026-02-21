package com.merdeleine.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.order.dto.CreateOrderRequest;
import com.merdeleine.order.dto.OrderResponse;
import com.merdeleine.order.dto.UpdateOrderRequest;
import com.merdeleine.order.entity.Order;
import com.merdeleine.order.entity.OutboxEvent;
import com.merdeleine.enums.OrderStatus;
import com.merdeleine.order.enums.OutboxEventStatus;
import com.merdeleine.order.mapper.OrderEventMapper;
import com.merdeleine.order.mapper.OrderMapper;
import com.merdeleine.order.repository.OrderRepository;
import com.merdeleine.order.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final QuotaService quotaService;
    private final String orderReservedTopic;

    public OrderService(OrderRepository orderRepository,
                        OutboxEventRepository outboxEventRepository,
                        ObjectMapper objectMapper,
                        QuotaService quotaService,
                        @Value("${app.kafka.topic.order-reserved-events}") String orderReservedTopic) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.quotaService = quotaService;
        this.orderReservedTopic = orderReservedTopic;

    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {

        // 1) 先原子扣額度（防超賣）
        quotaService.reserveOrThrow(
                request.sellWindowId(),
                request.productId(),
                request.quantity()
        );

        Order order = OrderMapper.toEntity(request, OrderStatus.RESERVED);
        Order saved = orderRepository.save(order);
        writeOutbox(
                "Order",
                saved.getId(),
                orderReservedTopic,
                new OrderEventMapper().toOrderEvent(saved, orderReservedTopic)
        );

        return OrderMapper.toResponse(saved);
    }

    @Transactional
    public OrderResponse get(UUID orderId) {
        Order order = findOrder(orderId);
        return OrderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse update(UUID orderId, UpdateOrderRequest req) {
        Order order = findOrder(orderId);

        if (req.contactName() != null) order.setContactName(req.contactName());
        if (req.contactPhone() != null) order.setContactPhone(req.contactPhone());
        if (req.contactEmail() != null) order.setContactEmail(req.contactEmail());
        if (req.shippingAddress() != null) order.setShippingAddress(req.shippingAddress());

        writeOutbox("ORDER", order.getId(), "order.updated.v1", new OrderEventMapper().toOrderEvent(order, "order.updated.v1"));

        return OrderMapper.toResponse(order);
    }

    @Transactional
    public void cancel(UUID orderId) {
        Order order = findOrder(orderId);
        order.setStatus(OrderStatus.CANCELLED);
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
}
