package com.merdeleine.production.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.messaging.OrderReservedEvent;
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

import java.util.UUID;

@Component
public class OrderReservedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderReservedEventConsumer.class);

    private final BatchCounterRepository batchCounterRepository;
    private final CounterEventLogRepository counterEventLogRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final String thresholdReachedTopic;



    public OrderReservedEventConsumer(BatchCounterRepository batchCounterRepository,
                                      CounterEventLogRepository counterEventLogRepository,
                                      OutboxEventRepository outboxEventRepository,
                                      ObjectMapper objectMapper,
                                      @Value("${app.kafka.topic.threshold-reached-events}") String thresholdReachedTopic
    ) {
        this.batchCounterRepository = batchCounterRepository;
        this.counterEventLogRepository = counterEventLogRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.thresholdReachedTopic = thresholdReachedTopic;
    }

    @KafkaListener(topics = "${kafka.topic.order-events:order.accumulated.events.v1}")
    @Transactional
    public void onMessage(OrderReservedEvent orderReservedEvent, Acknowledgment ack) {
        log.info("OrderAccumulatedEvent:" + orderReservedEvent.toString());

        if(counterEventLogRepository.existsBySourceEventId(orderReservedEvent.eventId())){
            log.info("Duplicate event detected, skipping processing for eventId: " + orderReservedEvent.eventId());
            ack.acknowledge();
            return;
        }


        BatchCounter batchCounter = batchCounterRepository
                .findBySellWindowIdAndProductId(orderReservedEvent.sellWindowId(), orderReservedEvent.productId())
                .orElseThrow(() -> new IllegalStateException(
                        String.format(
                            "BatchCounter not found for sellWindowId=%s, productId=%s",
                            orderReservedEvent.sellWindowId(),
                            orderReservedEvent.productId()
                        )
                    )
                );

        counterEventLogRepository
                .save(new CounterEventLogMapper().toCounterEventLog(orderReservedEvent, batchCounter));

        int newReservedQty = batchCounter.getReservedQty() + orderReservedEvent.quantity();
        batchCounter.setReservedQty(newReservedQty);
        if(newReservedQty >= batchCounter.getThresholdQty()
            && batchCounter.getStatus() == CounterStatus.OPEN) {
            batchCounter.setStatus(CounterStatus.REACHED);
            writeOutbox(
                    "BATCHCOUNTER",
                    batchCounter.getId(),
                    thresholdReachedTopic,
                    new ThresholdEventMapper().toThresholdReachedEvent(batchCounter, thresholdReachedTopic)
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
