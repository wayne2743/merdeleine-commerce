package com.merdeleine.catalog.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.catalog.entity.OutboxEvent;
import com.merdeleine.catalog.enums.OutboxEventStatus;
import com.merdeleine.catalog.repository.OutboxEventRepository;
import com.merdeleine.messaging.SellWindowQuotaConfiguredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class SellWindowQuotaOutboxPublisher {

    private final OutboxEventRepository outboxRepo;
    private final SellWindowQuotaConfiguredEventProducer producer;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final static Logger log = LoggerFactory.getLogger(SellWindowQuotaOutboxPublisher.class);

    public SellWindowQuotaOutboxPublisher(
            OutboxEventRepository outboxRepo,
            SellWindowQuotaConfiguredEventProducer producer,
            ObjectMapper objectMapper,
            @Value("${app.kafka.topic.sell-window-quota-configured:sell-window.quota.configured.v1}") String topic
    ) {
        this.outboxRepo = outboxRepo;
        this.producer = producer;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publish-interval-ms:1000}")
    @Transactional
    public void publish() {
        List<OutboxEvent> events = outboxRepo.findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.NEW);

        for (OutboxEvent e : events) {
            try {
                SellWindowQuotaConfiguredEvent event = objectMapper.treeToValue(e.getPayload(), SellWindowQuotaConfiguredEvent.class);
                String key = e.getAggregateId().toString();
                producer.publish(topic, key, event);

                e.setStatus(OutboxEventStatus.SENT);
                e.setSentAt(OffsetDateTime.now());
            } catch (Exception ex) {
                log.error("Failed to publish OutboxEvent id={}", e.getId(), ex);
                e.setStatus(OutboxEventStatus.FAILED);
                // 你表如果有 errorMessage 欄位可記一下（沒有就略過）
                // e.setErrorMessage(ex.getMessage());
            }
        }
    }
}
