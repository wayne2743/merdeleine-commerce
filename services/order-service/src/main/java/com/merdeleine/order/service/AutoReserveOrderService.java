package com.merdeleine.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.enums.OrderStatus;
import com.merdeleine.order.dto.AutoReserveOrderDtos;
import com.merdeleine.order.entity.Order;
import com.merdeleine.order.entity.OrderItem;
import com.merdeleine.order.entity.OutboxEvent;
import com.merdeleine.order.entity.SellWindowQuota;
import com.merdeleine.order.enums.OutboxEventStatus;
import com.merdeleine.order.mapper.OrderEventMapper;
import com.merdeleine.order.repository.OrderRepository;
import com.merdeleine.order.repository.OutboxEventRepository;
import com.merdeleine.order.repository.SellWindowQuotaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AutoReserveOrderService {

    private static final String QUOTA_OPEN = "OPEN";
    private static final String QUOTA_CLOSED = "CLOSED";

    private final OrderRepository orderRepository;
    private final SellWindowQuotaRepository quotaRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final String orderReservedTopic;

    public AutoReserveOrderService(
            OrderRepository orderRepository,
            SellWindowQuotaRepository quotaRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            @Value("${app.kafka.topic.order-reserved-events}") String orderReservedTopic
    ) {
        this.orderRepository = orderRepository;
        this.quotaRepository = quotaRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.orderReservedTopic = orderReservedTopic;
    }

    @Transactional
    public AutoReserveOrderDtos.Response autoReserve(AutoReserveOrderDtos.Request req) {

        // 1) 鎖 quota row（避免超賣）
        SellWindowQuota quota = quotaRepository.findForUpdate(req.sellWindowId(), req.productId())
                .orElseThrow(() -> new IllegalArgumentException("sell_window_quota not found"));

        if (!QUOTA_OPEN.equalsIgnoreCase(quota.getStatus())) {
            throw new IllegalStateException("quota is not OPEN");
        }

        int sold = quota.getSoldQty() == null ? 0 : quota.getSoldQty();
        int max  = quota.getMaxQty()  == null ? 0 : quota.getMaxQty();
        int qty  = req.qty();

        if (sold + qty > max) {
            throw new IllegalStateException("quota not enough");
        }

        // 2) 扣 quota
        int newSold = sold + qty;
        quota.setSoldQty(newSold);
        if (newSold >= max) {
            quota.setStatus(QUOTA_CLOSED);
        }

        // 3) 判斷是否已有同一 sell window 的既有 RESERVED 訂單
        UUID customerId = req.customerId();
        int  unitPriceCents = req.unitPriceCents();
        int  subtotalDelta  = qty * unitPriceCents;

        Order order = orderRepository
                .findActiveOrderByCustomerAndSellWindow(customerId, req.sellWindowId(), OrderStatus.RESERVED)
                .map(existing -> {
                    // --- 合併：把新增數量疊加到既有 order item ---
                    OrderItem item = existing.getItem();
                    item.setQuantity(item.getQuantity() + qty);
                    item.setSubtotalCents(item.getSubtotalCents() + subtotalDelta);
                    existing.setTotalAmountCents(existing.getTotalAmountCents() + subtotalDelta);
                    return existing;
                })
                .orElseGet(() -> {
                    // --- 建新單 ---
                    Order newOrder = new Order(
                            UUID.randomUUID(),
                            generateOrderNo(),
                            customerId,
                            OrderStatus.RESERVED,
                            subtotalDelta,
                            req.currency()
                    );
                    newOrder.setSellWindowId(req.sellWindowId());
                    newOrder.setShippingAddress(req.shippingAddress());

                    OrderItem item = new OrderItem(
                            UUID.randomUUID(),
                            req.productId(),
                            qty,
                            unitPriceCents,
                            subtotalDelta
                    );
                    newOrder.setItem(item);
                    return orderRepository.save(newOrder);
                });

        // 4) 不論新建或合併，都只發「本次 delta qty」給 threshold
        writeOutbox(
                "Order",
                order.getId(),
                orderReservedTopic,
                new OrderEventMapper().toOrderEvent(order, orderReservedTopic, qty)
        );

        return new AutoReserveOrderDtos.Response(order.getId(), order.getStatus().name());
    }

    private String generateOrderNo() {
        // MVP：時間 + 亂數；你之後可改成序號服務
        long ts = System.currentTimeMillis();
        String rand = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "O" + ts + "-" + rand;
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