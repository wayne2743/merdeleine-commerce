package com.merdeleine.production.messaging;

import com.merdeleine.messaging.OrderEvent;
import com.merdeleine.production.repository.BatchCounterRepository;
import com.merdeleine.production.repository.CounterEventLogRepository;
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
    public void onMessage(OrderEvent orderEvent, Acknowledgment ack) {
        log.info("orderEvent:" +orderEvent.toString());
        orderEvent.lines().forEach(line -> {
            log.info("Processing line - productId: {}, variantId: {}, quantity: {}",
                    line.productId(), line.variantId(), line.quantity());

            line.productId()
        });
//        counterEventLogRepository.saveFromOrderEvent(orderEvent);
//        batchCounterRepository.save();
        ack.acknowledge();
    }
}
