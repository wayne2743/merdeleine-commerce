package com.merdeleine.production.planning.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.messaging.BatchConfirmEvent;
import com.merdeleine.messaging.BatchCreatedNotificationEvent;
import com.merdeleine.production.planning.entity.OutboxEvent;
import com.merdeleine.production.planning.enums.OutboxEventStatus;
import com.merdeleine.production.planning.repository.OutboxEventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class BatchPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final BatchEventProducer producer;
    private final ObjectMapper objectMapper;


    public BatchPublisher(
            OutboxEventRepository outboxEventRepository,
            BatchEventProducer producer,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.producer = producer;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publish-interval-ms:1000}")
    @Transactional
    public void publish() {
        List<OutboxEvent> events = outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.NEW);

        for (OutboxEvent e : events) {
            try {
                Object event;
                switch (e.getEventType()) {
                    case "batch.confirmed.v1" ->
                            event = objectMapper.treeToValue(e.getPayload(), BatchConfirmEvent.class);
                    case "batch.created.notification.v1" ->
                            event = objectMapper.treeToValue(e.getPayload(), BatchCreatedNotificationEvent.class);
                    default ->
                            throw new IllegalStateException("Unknown eventType: " + e.getEventType());
                }

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
