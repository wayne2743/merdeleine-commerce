package com.merdeleine.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.order.dto.OrderCreateRequest;
import com.merdeleine.order.dto.OrderItemRequest;
import com.merdeleine.order.dto.OrderResponse;
import com.merdeleine.order.dto.OrderUpdateRequest;
import com.merdeleine.order.entity.Order;
import com.merdeleine.order.entity.OrderItem;
import com.merdeleine.order.entity.OutboxEvent;
import com.merdeleine.order.enums.OrderStatus;
import com.merdeleine.order.enums.OutboxEventStatus;
import com.merdeleine.order.exception.BadRequestException;
import com.merdeleine.order.exception.ResourceNotFoundException;
import com.merdeleine.order.mapper.OrderEventMapper;
import com.merdeleine.order.mapper.OrderResponseMapper;
import com.merdeleine.order.repository.OrderRepository;
import com.merdeleine.order.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OrderService(
            OrderRepository orderRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderResponse create(OrderCreateRequest req) {
        UUID orderId = UUID.randomUUID();

        Order order = new Order();
        order.setId(orderId);
        order.setOrderNo(generateOrderNo());
        order.setCustomerId(req.customerId());
        order.setSellWindowId(req.sellWindowId());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setCurrency(req.currency());
        order.setContactName(req.contactName());
        order.setContactPhone(req.contactPhone());
        order.setContactEmail(req.contactEmail());
        order.setShippingAddress(req.shippingAddress());

        int total = 0;
        for (OrderItemRequest itemReq : req.items()) {
            OrderItem item = new OrderItem();
            item.setId(UUID.randomUUID());
            item.setProductId(itemReq.productId());
            item.setVariantId(itemReq.variantId());
            item.setQuantity(itemReq.quantity());
            item.setUnitPriceCents(itemReq.unitPriceCents());
            int subtotal = itemReq.quantity() * itemReq.unitPriceCents();
            item.setSubtotalCents(subtotal);

            total += subtotal;
            order.addItem(item);
        }
        order.setTotalAmountCents(total);

        Order saved = orderRepository.save(order);

        // Outbox: order.created.v1

        writeOutbox("ORDER", saved.getId(), "order.created.v1", new OrderEventMapper().toOrderEvent(saved));

        return new OrderResponseMapper().toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        return new OrderResponseMapper().toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> list() {
        return orderRepository.findAll().stream()
                .map(o -> new OrderResponseMapper().toResponse(o))
                .toList();
    }

    @Transactional
    public OrderResponse update(UUID id, OrderUpdateRequest req) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REFUNDED) {
            throw new BadRequestException("Cannot update order in status: " + order.getStatus());
        }

        order.setSellWindowId(req.sellWindowId());
        order.setContactName(req.contactName());
        order.setContactPhone(req.contactPhone());
        order.setContactEmail(req.contactEmail());
        order.setShippingAddress(req.shippingAddress());

        // replace items
        order.clearItems();
        int total = 0;
        for (OrderItemRequest itemReq : req.items()) {
            OrderItem item = new OrderItem();
            item.setId(UUID.randomUUID());
            item.setProductId(itemReq.productId());
            item.setVariantId(itemReq.variantId());
            item.setQuantity(itemReq.quantity());
            item.setUnitPriceCents(itemReq.unitPriceCents());
            int subtotal = itemReq.quantity() * itemReq.unitPriceCents();
            item.setSubtotalCents(subtotal);
            total += subtotal;
            order.addItem(item);
        }
        order.setTotalAmountCents(total);

        Order saved = orderRepository.save(order);
        // Outbox: order.updated.v1

        writeOutbox("ORDER", saved.getId(), "order.updated.v1", new OrderEventMapper().toOrderEvent(saved));
        return new OrderResponseMapper().toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));

        // 實務上通常不 hard delete；此處示範 CRUD
        orderRepository.delete(order);

        // 你也可寫 outbox: order.deleted.v1（視需求）
        writeOutbox("ORDER", id, "order.deleted.v1", new DeletePayload(id));
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

    private String generateOrderNo() {
        // 簡單示範：實務可用 Snowflake/DB sequence/日期+random
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private record DeletePayload(UUID orderId) {}
}
