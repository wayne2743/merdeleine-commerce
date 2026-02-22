package com.merdeleine.order.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.messaging.PaymentCompletedEvent;
import com.merdeleine.messaging.PaymentPaidEvent;
import com.merdeleine.order.entity.Order;
import com.merdeleine.order.entity.OutboxEvent;
import com.merdeleine.enums.OrderStatus;
import com.merdeleine.order.enums.OutboxEventStatus;
import com.merdeleine.order.repository.OrderRepository;
import com.merdeleine.order.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.UUID;


@Component
public class PaymentCompletedConsumer {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;
    private final String paymentPaidTopic;

    private final Logger log = LoggerFactory.getLogger(PaymentCompletedConsumer.class);

    public PaymentCompletedConsumer(OrderRepository orderRepository,
                                    ObjectMapper objectMapper,
                                    OutboxEventRepository outboxEventRepository,
                                    @Value("${app.kafka.topic.payment-paid-events}") String paymentPaidTopic
                                   ) {
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
        this.outboxEventRepository = outboxEventRepository;
        this.paymentPaidTopic = paymentPaidTopic;
    }

    @KafkaListener(
            topics = "${app.kafka.topic.payment-completed-events}",
            groupId = "${app.kafka.consumer.group-id}"
    )
    @Transactional
    public void onMessage(
            PaymentCompletedEvent event,
            Acknowledgment ack
    ) {
        log.info(
                "[PaymentCompleted] eventId={}, orderId={}, paymentStatus={}",
                event.eventId(),
                event.orderId(),
                event.paymentStatus()
        );

        Order order = orderRepository.findById(event.orderId()).orElseThrow(
                () -> new RuntimeException("Order not found for orderId: " + event.orderId())
        );
        switch (event.paymentStatus()) {
            case SUCCEEDED -> order.setStatus(OrderStatus.PAID);
            case FAILED -> order.setStatus(OrderStatus.PAYMENT_REQUESTED);
            default -> {
                log.warn("[PaymentCompleted] unknown paymentStatus={}, orderId={}", event.paymentStatus(), event.orderId());
            }
        }
        writeOutbox(
                "Order",
                order.getId(),
                paymentPaidTopic,
                new PaymentPaidEvent(
                        UUID.randomUUID(),
                        paymentPaidTopic,
                        order.getId(),
                        order.getSellWindowId(),
                        order.getItem().getProductId(),
                        order.getItem().getQuantity(),
                        order.getStatus(),
                        OffsetDateTime.now()
                )
        );

        // ✅ 重點：等交易「真的 commit」成功後才 ack
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    ack.acknowledge();
                }
            });
        } else {
            // 理論上在 @Transactional 內會 active；保底用
            ack.acknowledge();
        }

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

