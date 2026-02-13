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
    public void onMessage(
            ThresholdReachedEvent event,
            Acknowledgment ack
    ) {
        // 先檢查是否已經有 batch 存在，避免重複處理同一個 event
        batchRepository.findByProductIdAndSellWindowId(
                event.productId(),
                event.sellWindowId()
        ).ifPresent(v-> {;
            log.warn(
                    "Batch already exists for productId={} and sellWindowId={}, skipping. existing batchId={}",
                    event.productId(),
                    event.sellWindowId(),
                    v.getId()
            );
            ack.acknowledge();
            return;
        });

        log.info(
                "[QuotaConfigured] eventId={}, sellWindowId={}, productId={}, counterId={}",
                event.eventId(),
                event.sellWindowId(),
                event.productId(),
                event.counterId()
        );
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
