package com.merdeleine.production.planning.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.production.planning.entity.OutboxEvent;
import com.merdeleine.production.planning.enums.OutboxEventStatus;
import com.merdeleine.production.planning.repository.OutboxEventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class BatchPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final BatchEventProducer producer;
    private final ObjectMapper objectMapper;


    public BatchPublisher(
            OutboxEventRepository outboxEventRepository,
            BatchEventProducer producer,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.producer = producer;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publish-interval-ms:1000}")
    @Transactional
    public void publish() {
        List<OutboxEvent> events = outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.NEW);

        for (OutboxEvent e : events) {
            try {
                // 你 outbox.payload 是 json string，這裡把它轉成你要送的 DTO 或 Map
                // 做法 A：直接送 payload(Map)（最快）
                String payload = objectMapper.writeValueAsString(e.getPayload());

                String key = e.getAggregateId().toString();
                producer.publish(e.getEventType(), key, payload);

                e.setStatus(OutboxEventStatus.SENT);
                e.setSentAt(OffsetDateTime.now());
            } catch (Exception ex) {
                // 可視需求改成 FAILED + retry_count（你表目前沒有 retry_count）
                e.setStatus(OutboxEventStatus.FAILED);
            }
        }
        // 交易結束後，狀態更新會一起 commit
    }

}
