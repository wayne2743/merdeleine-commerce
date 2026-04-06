package com.merdeleine.production.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.enums.OrderStatus;
import com.merdeleine.messaging.PaymentPaidEvent;
import com.merdeleine.messaging.PaymentPaidNotificationEvent;
import com.merdeleine.messaging.ThresholdReachedEvent;
import com.merdeleine.production.entity.BatchCounter;
import com.merdeleine.production.entity.OutboxEvent;
import com.merdeleine.production.enums.CounterStatus;
import com.merdeleine.production.enums.OutboxEventStatus;
import com.merdeleine.production.mapper.CounterEventLogMapper;
import com.merdeleine.production.mapper.ThresholdEventMapper;
import com.merdeleine.production.repository.BatchCounterRepository;
import com.merdeleine.production.repository.CounterEventLogRepository;
import com.merdeleine.production.repository.OutboxEventRepository;
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
public class PaymentPaidEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentPaidEventConsumer.class);

    private final BatchCounterRepository batchCounterRepository;
    private final CounterEventLogRepository counterEventLogRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final String thresholdReachedTopic;
    private final String paymentPaidNotificationTopic;

    public PaymentPaidEventConsumer(BatchCounterRepository batchCounterRepository,
                                    CounterEventLogRepository counterEventLogRepository,
                                    OutboxEventRepository outboxEventRepository,
                                    ObjectMapper objectMapper,
                                    @Value("${app.kafka.topic.threshold-reached-events}") String thresholdReachedTopic,
                                    @Value("${app.kafka.topic.payment-paid-notification-events}") String paymentPaidNotificationTopic
    ) {
        this.batchCounterRepository = batchCounterRepository;
        this.counterEventLogRepository = counterEventLogRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.thresholdReachedTopic = thresholdReachedTopic;
        this.paymentPaidNotificationTopic = paymentPaidNotificationTopic;
    }

    @KafkaListener(topics = "${app.kafka.topic.payment-paid-events}")
    @Transactional
    public void onMessage(PaymentPaidEvent event, Acknowledgment ack) {
        log.info("PaymentPaidEvent:{}", event);

        if (counterEventLogRepository.existsBySourceEventId(event.eventId())) {
            log.info("Duplicate event detected, skipping processing for eventId: {}", event.eventId());
            ack.acknowledge();
            return;
        }

        if (event.orderStatus() != OrderStatus.PAID) {
            log.info("Order status is not PAID, skipping processing for eventId: {}", event.eventId());
            ack.acknowledge();
            return;
        }

        BatchCounter batchCounter = batchCounterRepository
                .findBySellWindowIdAndProductId(event.sellWindowId(), event.productId())
                .orElseThrow(() -> new IllegalStateException(
                        String.format("BatchCounter not found for sellWindowId=%s, productId=%s",
                                event.sellWindowId(), event.productId())
                ));

        counterEventLogRepository
                .save(new CounterEventLogMapper().toCounterEventLog(event, batchCounter));

        int newPaidQty = batchCounter.getPaidQty() + event.quantity();
        batchCounter.setPaidQty(newPaidQty);

        // 首次到量：發 ThresholdReachedEvent 給 production-planning
        if (newPaidQty >= batchCounter.getThresholdQty() && batchCounter.getReachedAt() == null) {
            batchCounter.setReachedAt(OffsetDateTime.now());
            batchCounter.setReachedEventId(event.eventId());
            batchCounter.setStatus(CounterStatus.REACHED);

            ThresholdReachedEvent thresholdReachedEvent =
                    new ThresholdEventMapper().toThresholdReachedEvent(batchCounter, thresholdReachedTopic);
            writeOutbox("BatchCounter", batchCounter.getId(), thresholdReachedTopic, thresholdReachedEvent);

            log.info("[ThresholdReached] sellWindowId={}, productId={}, paidQty={}, thresholdQty={}",
                    batchCounter.getSellWindowId(), batchCounter.getProductId(),
                    newPaidQty, batchCounter.getThresholdQty());
        }

        // 每筆付款成功都通知 notification-service（銀行轉帳入帳確認信）
        writeOutbox("BatchCounter", batchCounter.getId(), paymentPaidNotificationTopic,
                new PaymentPaidNotificationEvent(
                        UUID.randomUUID(),
                        paymentPaidNotificationTopic,
                        event.orderId(),
                        event.customerId(),
                        OffsetDateTime.now()
                ));

        batchCounterRepository.save(batchCounter);

        // 等 transaction commit 後再 ack
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    ack.acknowledge();
                }
            });
        } else {
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
            throw new RuntimeException("Failed to write outbox event", e);
        }
    }
}
