package com.merdeleine.order.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.messaging.OrderReservedEvent;
import com.merdeleine.messaging.PaymentRequestedEvent;
import com.merdeleine.order.entity.OutboxEvent;
import com.merdeleine.order.enums.OutboxEventStatus;
import com.merdeleine.order.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class OutboxPublisher {

    private final OutboxEventRepository outboxRepo;
    private final OrderEventProducer producer;
    private final ObjectMapper objectMapper;
    private final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    public OutboxPublisher(
            OutboxEventRepository outboxRepo,
            OrderEventProducer producer, ObjectMapper objectMapper) {
        this.outboxRepo = outboxRepo;
        this.producer = producer;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publish-interval-ms:1000}")
    @Transactional
    public void publish() {
        List<OutboxEvent> events = outboxRepo.findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.NEW);

        for (OutboxEvent e : events) {
            try {
                Object event;
                switch (e.getEventType()) {
                    case "order.reserved.v1" ->
                            event = objectMapper.treeToValue(e.getPayload(), OrderReservedEvent.class);
                    case "payment.requested.v1" ->
                            event = objectMapper.treeToValue(e.getPayload(), PaymentRequestedEvent.class);
                    default ->
                            throw new IllegalStateException("Unknown eventType: " + e.getEventType());
                }
                String key = e.getAggregateId().toString();
                producer.publish(e.getEventType(), key, event);

                e.setStatus(OutboxEventStatus.SENT);
                e.setSentAt(OffsetDateTime.now());
            } catch (Exception ex) {
                log.error("Failed to publish outbox event id=" + e.getId(), ex);
                // 可視需求改成 FAILED + retry_count（你表目前沒有 retry_count）
                e.setStatus(OutboxEventStatus.FAILED);
            }
        }
        // 交易結束後，狀態更新會一起 commit
    }
}
