package com.merdeleine.production.messaging;

import com.merdeleine.messaging.OrderCancelledEvent;
import com.merdeleine.production.entity.BatchCounter;
import com.merdeleine.production.mapper.CounterEventLogMapper;
import com.merdeleine.production.repository.BatchCounterRepository;
import com.merdeleine.production.repository.CounterEventLogRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class OrderCancelledEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderCancelledEventConsumer.class);

    private final BatchCounterRepository batchCounterRepository;
    private final CounterEventLogRepository counterEventLogRepository;

    public OrderCancelledEventConsumer(BatchCounterRepository batchCounterRepository,
                                       CounterEventLogRepository counterEventLogRepository) {
        this.batchCounterRepository = batchCounterRepository;
        this.counterEventLogRepository = counterEventLogRepository;
    }

    @KafkaListener(topics = "${app.kafka.topic.order-cancelled-events}")
    @Transactional
    public void onMessage(OrderCancelledEvent event, Acknowledgment ack) {
        log.info("OrderCancelledEvent: {}", event);

        if (counterEventLogRepository.existsBySourceEventId(event.eventId())) {
            log.info("Duplicate event detected, skipping processing for eventId: {}", event.eventId());
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
                ));

        counterEventLogRepository
                .save(new CounterEventLogMapper().toReverseCounterEventLog(event, batchCounter));

        int currentReserved = batchCounter.getReservedQty() == null ? 0 : batchCounter.getReservedQty();
        int nextReserved = Math.max(0, currentReserved - event.quantity());
        batchCounter.setReservedQty(nextReserved);

        ack.acknowledge();
    }
}

