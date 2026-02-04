package com.merdeleine.catalog.messaging;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class SellWindowQuotaConfiguredEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SellWindowQuotaConfiguredEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String topic, String key, Object event) {
        kafkaTemplate.send(topic, key, event);
    }
}
