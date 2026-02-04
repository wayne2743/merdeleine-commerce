package com.merdeleine.order.messaging;

import com.merdeleine.order.entity.OutboxEvent;
import com.merdeleine.order.enums.OutboxEventStatus;
import com.merdeleine.order.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class OutboxPublisher {

    private final OutboxEventRepository outboxRepo;
    private final OrderEventProducer producer;
    private final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final String topic;

    public OutboxPublisher(
            OutboxEventRepository outboxRepo,
            OrderEventProducer producer,
            @Value("${app.kafka.topic.order-events:order.accumulated.events.v1}") String topic
    ) {
        this.outboxRepo = outboxRepo;
        this.producer = producer;
        this.topic = topic;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publish-interval-ms:1000}")
    @Transactional
    public void publish() {
        List<OutboxEvent> events = outboxRepo.findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.NEW);

        for (OutboxEvent e : events) {
            try {
                // 你 outbox.payload 是 json string，這裡把它轉成你要送的 DTO 或 Map
                // 做法 A：直接送 payload(Map)（最快）
//                String payload = objectMapper.writeValueAsString(e.getPayload());

                String key = e.getAggregateId().toString();
                producer.publish(topic, key, e.getPayload());

                e.setStatus(OutboxEventStatus.SENT);
                e.setSentAt(OffsetDateTime.now());
            } catch (Exception ex) {
                log.error("Failed to publish outbox event id=" + e.getId(), ex);
                // 可視需求改成 FAILED + retry_count（你表目前沒有 retry_count）
                e.setStatus(OutboxEventStatus.FAILED);
            }
        }
        // 交易結束後，狀態更新會一起 commit
    }
}
