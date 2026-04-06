package com.merdeleine.production.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.messaging.PaymentPaidNotificationEvent;
import com.merdeleine.messaging.ThresholdReachedEvent;
import com.merdeleine.production.entity.OutboxEvent;
import com.merdeleine.production.enums.OutboxEventStatus;
import com.merdeleine.production.repository.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class ThresholdPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ThresholdEventProducer producer;
    private final ObjectMapper objectMapper;

    private final String topic;

    public ThresholdPublisher(
            OutboxEventRepository outboxEventRepository,
            ThresholdEventProducer producer,
            ObjectMapper objectMapper,
            @Value("${app.kafka.topic.threshold-reached-events}") String topic
    ) {

        this.outboxEventRepository = outboxEventRepository;
        this.producer = producer;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publish-interval-ms:1000}")
    @Transactional
    public void publish() {
        List<OutboxEvent> events = outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.NEW);

        for (OutboxEvent e : events) {
            try {
                Object event;
                switch (e.getEventType()) {
                    case "threshold.reached.v1" ->
                            event = objectMapper.treeToValue(e.getPayload(), ThresholdReachedEvent.class);
                    case "payment.paid.notification.v1" ->
                            event = objectMapper.treeToValue(e.getPayload(), PaymentPaidNotificationEvent.class);
                    default ->
                            throw new IllegalStateException("Unknown eventType: " + e.getEventType());
                }
//                String payload = objectMapper.writeValueAsString(e.getPayload());

                String key = e.getAggregateId().toString();
                producer.publish(e.getEventType(), key, event);

                e.setStatus(OutboxEventStatus.SENT);
                e.setSentAt(OffsetDateTime.now());
            } catch (Exception ex) {
                // 可視需求改成 FAILED + retry_count（你表目前沒有 retry_count）
                e.setStatus(OutboxEventStatus.FAILED);
            }
        }
        // 交易結束後，狀態更新會一起 commit
    }

}
