package com.merdeleine.production.messaging;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ThresholdEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ThresholdEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String topic, String key, Object event) {
        kafkaTemplate.send(topic, key, event);
    }
}
