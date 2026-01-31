package com.merdeleine.production.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.messaging.OrderAccumulatedEvent;
import com.merdeleine.production.entity.BatchCounter;
import com.merdeleine.production.entity.OutboxEvent;
import com.merdeleine.production.enums.BatchCounterStatus;
import com.merdeleine.production.enums.OutboxEventStatus;
import com.merdeleine.production.mapper.CounterEventLogMapper;
import com.merdeleine.production.mapper.ThresholdEventMapper;
import com.merdeleine.production.repository.BatchCounterRepository;
import com.merdeleine.production.repository.CounterEventLogRepository;
import com.merdeleine.production.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrderAccumulatedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderAccumulatedEventConsumer.class);

    private final BatchCounterRepository batchCounterRepository;
    private final CounterEventLogRepository counterEventLogRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;



    public OrderAccumulatedEventConsumer(BatchCounterRepository batchCounterRepository, CounterEventLogRepository counterEventLogRepository, OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.batchCounterRepository = batchCounterRepository;
        this.counterEventLogRepository = counterEventLogRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${kafka.topic.order-events:order.accumulated.events.v1}")
    @Transactional
    public void onMessage(OrderAccumulatedEvent orderAccumulatedEvent, Acknowledgment ack) {
        log.info("OrderAccumulatedEvent:" + orderAccumulatedEvent.toString());

        if(counterEventLogRepository.existsBySourceEventId(orderAccumulatedEvent.eventId())){
            log.info("Duplicate event detected, skipping processing for eventId: " + orderAccumulatedEvent.eventId());
            ack.acknowledge();
            return;
        }


        BatchCounter batchCounter = batchCounterRepository
                .findBySellWindowIdAndProductId(orderAccumulatedEvent.sellWindowId(), orderAccumulatedEvent.productId())
                .orElseThrow(() -> new IllegalStateException(
                        String.format(
                            "BatchCounter not found for sellWindowId=%s, productId=%s",
                            orderAccumulatedEvent.sellWindowId(),
                            orderAccumulatedEvent.productId()
                        )
                    )
                );

        counterEventLogRepository
                .save(new CounterEventLogMapper().toCounterEventLog(orderAccumulatedEvent, batchCounter));

        int newPaidQty = batchCounter.getPaidQty() + orderAccumulatedEvent.quantity();
        batchCounter.setPaidQty(newPaidQty);
        if(newPaidQty >= batchCounter.getThresholdQty()){
            batchCounter.setStatus(BatchCounterStatus.REACHED);
            writeOutbox(
                    "BATCHCOUNTER",
                    batchCounter.getId(),
                    "threshold.reached.v1",
                    new ThresholdEventMapper().toThresholdReachedEvent(batchCounter)
            );
        }
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
