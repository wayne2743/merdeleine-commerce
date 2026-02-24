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
        int max = quota.getMaxQty() == null ? 0 : quota.getMaxQty();
        int qty = req.qty();

        if (sold + qty > max) {
            throw new IllegalStateException("quota not enough");
        }

        // 2) 扣 quota
        int newSold = sold + qty;
        quota.setSoldQty(newSold);
        if (newSold == max) {
            quota.setStatus(QUOTA_CLOSED);
        }
        // quotaRepository.save(quota); // 可省略，交易內 managed entity dirty checking 會更新

        // 3) 建立 Order / OrderItem
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID(); // MVP：先用系統生成；之後接登入再改

        int unitPriceCents = 0;              // MVP：先 0；之後可由 catalog 傳入或查價
        String currency = "TWD";

        Order order = new Order(
                orderId,
                generateOrderNo(),
                customerId,
                OrderStatus.RESERVED,
                qty * unitPriceCents,
                currency
        );
        order.setSellWindowId(req.sellWindowId());
        order.setContactName(req.contactName());
        order.setContactPhone(req.contactPhone());
        order.setContactEmail(req.contactEmail());
        order.setShippingAddress(req.shippingAddress());

        OrderItem item = new OrderItem(
                UUID.randomUUID(),
                req.productId(),
                qty,
                unitPriceCents,
                qty * unitPriceCents
        );
        order.setItem(item);

        orderRepository.save(order);

        writeOutbox(
                "Order",
                order.getId(),
                orderReservedTopic,
                new OrderEventMapper().toOrderEvent(order, orderReservedTopic)
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