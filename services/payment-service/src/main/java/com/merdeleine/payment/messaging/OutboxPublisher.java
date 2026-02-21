package com.merdeleine.payment.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.messaging.PaymentCreatedEvent;
import com.merdeleine.messaging.PaymentCompletedEvent;
import com.merdeleine.payment.entity.OutboxEvent;
import com.merdeleine.payment.enums.OutboxEventStatus;
import com.merdeleine.payment.repository.OutboxEventRepository;
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
    private final PaymentEventProducer producer;
    private final ObjectMapper objectMapper;
    private final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    public OutboxPublisher(
            OutboxEventRepository outboxRepo,
            PaymentEventProducer producer,
            ObjectMapper objectMapper) {
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
                    case "payment.created.v1" ->
                            event = objectMapper.treeToValue(e.getPayload(), PaymentCreatedEvent.class);
                    case "payment.completed.v1" ->
                            event = objectMapper.treeToValue(e.getPayload(), PaymentCompletedEvent.class);
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
