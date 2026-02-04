package com.merdeleine.production.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.messaging.ThresholdReachedEvent;
import com.merdeleine.production.entity.Batch;
import com.merdeleine.production.entity.OutboxEvent;
import com.merdeleine.production.enums.BatchStatus;
import com.merdeleine.production.enums.OutboxEventStatus;
import com.merdeleine.production.mapper.BatchMappers;
import com.merdeleine.production.repository.BatchRepository;
import com.merdeleine.production.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.UUID;


@Component
public class ThresholdReachedConsumer {

    private final Logger log = LoggerFactory.getLogger(ThresholdReachedConsumer.class);

    private final BatchRepository batchRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public ThresholdReachedConsumer(BatchRepository batchRepository, OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {

        this.batchRepository = batchRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topic.sell-window-quota-configured}",
            groupId = "${app.kafka.consumer.group-id}"
    )
    public void onMessage(
            ThresholdReachedEvent event,
            Acknowledgment ack
    ) {
        log.info(
                "[QuotaConfigured] eventId={}, sellWindowId={}, productId={}, counterId={}",
                event.eventId(),
                event.sellWindowId(),
                event.productId(),
                event.counterId()
        );
        Batch saved = batchRepository.save(BatchMappers.toBatch(event, BatchStatus.CREATED));

        writeOutbox(
                "Batch",
                event.eventId(),
                "batch.created.notification.v1",
                BatchMappers.toBatchCreatedNotificationEvent(event, saved)
        );
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
