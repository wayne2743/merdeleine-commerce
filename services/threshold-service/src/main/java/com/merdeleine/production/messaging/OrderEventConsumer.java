package com.merdeleine.production.messaging;

import com.merdeleine.messaging.OrderEvent;
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
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final BatchCounterRepository batchCounterRepository;
    private final CounterEventLogRepository counterEventLogRepository;


    public OrderEventConsumer(BatchCounterRepository batchCounterRepository, CounterEventLogRepository counterEventLogRepository) {
        this.batchCounterRepository = batchCounterRepository;
        this.counterEventLogRepository = counterEventLogRepository;
    }

    @KafkaListener(topics = "${kafka.topic.order-events:order.events.v1}")
    @Transactional
    public void onMessage(OrderEvent orderEvent, Acknowledgment ack) {
        log.info("orderEvent:" +orderEvent.toString());


        counterEventLogRepository.save(new CounterEventLogMapper().toCounterEventLog(orderEvent));
        BatchCounter batchCounter = batchCounterRepository
                .findBySellWindowIdAndProductId(orderEvent.sellWindowId(), orderEvent.productId())
                .orElseThrow(() -> new IllegalStateException(
                        String.format(
                            "BatchCounter not found for sellWindowId=%s, productId=%s",
                            orderEvent.sellWindowId(),
                            orderEvent.productId()
                        )
                    )
                );
        batchCounter.setPaidQty(batchCounter.getPaidQty() + orderEvent.quantity());

        ack.acknowledge();
    }
}
