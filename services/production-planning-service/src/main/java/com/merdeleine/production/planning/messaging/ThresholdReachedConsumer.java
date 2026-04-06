package com.merdeleine.production.planning.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.messaging.ThresholdReachedEvent;
import com.merdeleine.production.planning.entity.Batch;
import com.merdeleine.production.planning.entity.OutboxEvent;
import com.merdeleine.production.planning.enums.BatchStatus;
import com.merdeleine.production.planning.enums.OutboxEventStatus;
import com.merdeleine.production.planning.mapper.BatchMappers;
import com.merdeleine.production.planning.repository.BatchRepository;
import com.merdeleine.production.planning.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import java.util.UUID;


@Component
public class ThresholdReachedConsumer {

    private final Logger log = LoggerFactory.getLogger(ThresholdReachedConsumer.class);

    private final BatchRepository batchRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final String batchCreatedNotificationTopic;


    public ThresholdReachedConsumer(BatchRepository batchRepository,
                                    OutboxEventRepository outboxEventRepository,
                                    ObjectMapper objectMapper,
                                    @Value("${app.kafka.topic.notification-events}") String batchCreatedNotificationTopic) {

        this.batchRepository = batchRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.batchCreatedNotificationTopic = batchCreatedNotificationTopic;
    }

    @KafkaListener(
            topics = "${app.kafka.topic.threshold-reached-events}",
            groupId = "${app.kafka.consumer.group-id}"
    )
    @Transactional
    public void onMessage(
            ThresholdReachedEvent event,
            Acknowledgment ack
    ) {
        log.info(
                "[ThresholdReached] eventId={}, sellWindowId={}, productId={}, counterId={}",
                event.eventId(),
                event.sellWindowId(),
                event.productId(),
                event.counterId()
        );

        // 若 batch 已存在（正常情況：admin 已確認過），直接更新狀態為 PAID
        var existing = batchRepository.findByProductIdAndSellWindowId(
                event.productId(),
                event.sellWindowId()
        );

        if (existing.isPresent()) {
            Batch batch = existing.get();
            if (batch.getStatus() != BatchStatus.CANCELLED) {
                batch.setStatus(BatchStatus.PAID);
                batchRepository.save(batch);
                log.info("[ThresholdReached] updated existing batch={} to PAID", batch.getId());
            } else {
                log.warn("[ThresholdReached] batch={} is CANCELLED, skipping", batch.getId());
            }
            ack.acknowledge();
            return;
        }

        // Batch 不存在時建立（fallback，正常流程不應走到這裡）
        Batch saved = batchRepository.save(BatchMappers.toBatch(event, BatchStatus.CREATED, UUID.randomUUID()));

        writeOutbox(
                "Batch",
                event.eventId(),
                batchCreatedNotificationTopic,
                BatchMappers.toBatchCreatedNotificationEvent(event, saved)
        );
        ack.acknowledge();
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
