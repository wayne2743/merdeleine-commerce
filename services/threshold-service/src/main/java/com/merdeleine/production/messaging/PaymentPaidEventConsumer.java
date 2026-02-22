package com.merdeleine.production.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.enums.OrderStatus;
import com.merdeleine.messaging.PaymentPaidEvent;
import com.merdeleine.production.entity.BatchCounter;
import com.merdeleine.production.entity.OutboxEvent;
import com.merdeleine.production.enums.OutboxEventStatus;
import com.merdeleine.production.mapper.CounterEventLogMapper;
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
public class PaymentPaidEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentPaidEventConsumer.class);

    private final BatchCounterRepository batchCounterRepository;
    private final CounterEventLogRepository counterEventLogRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;



    public PaymentPaidEventConsumer(BatchCounterRepository batchCounterRepository,
                                    CounterEventLogRepository counterEventLogRepository,
                                    OutboxEventRepository outboxEventRepository,
                                    ObjectMapper objectMapper
    ) {
        this.batchCounterRepository = batchCounterRepository;
        this.counterEventLogRepository = counterEventLogRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topic.order-reserved-events}")
    @Transactional
    public void onMessage(PaymentPaidEvent event, Acknowledgment ack) {
        log.info("OrderReservedEvent:" + event.toString());

        if(counterEventLogRepository.existsBySourceEventId(event.eventId())){
            log.info("Duplicate event detected, skipping processing for eventId: " + event.eventId());
            ack.acknowledge();
            return;
        }

        if(event.orderStatus() != OrderStatus.PAID) {
            log.info("Order status is not PAID, skipping processing for eventId: " + event.eventId());
            ack.acknowledge();
            return;
        }

        BatchCounter batchCounter = batchCounterRepository
                .findBySellWindowIdAndProductId(event.sellWindowId(), event.productId())
                .orElseThrow(() -> new IllegalStateException(
                        String.format(
                            "BatchCounter not found for sellWindowId=%s, productId=%s",
                                event.sellWindowId(),
                                event.productId()
                        )
                    )
                );

        counterEventLogRepository
                .save(new CounterEventLogMapper().toCounterEventLog(event, batchCounter));

        int newPaidQty = batchCounter.getPaidQty() + event.quantity();
        batchCounter.setPaidQty(newPaidQty);

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
