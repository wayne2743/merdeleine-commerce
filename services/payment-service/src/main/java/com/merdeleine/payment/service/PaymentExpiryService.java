package com.merdeleine.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.enums.PaymentStatus;
import com.merdeleine.messaging.PaymentExpiredEvent;
import com.merdeleine.payment.entity.OutboxEvent;
import com.merdeleine.payment.entity.Payment;
import com.merdeleine.payment.enums.OutboxEventStatus;
import com.merdeleine.payment.repository.OutboxEventRepository;
import com.merdeleine.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentExpiryService {

    private final PaymentRepository paymentRepository;
    private final String paymentExpiredTopic;
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;

    public PaymentExpiryService(PaymentRepository paymentRepository,
                                @Value("${app.kafka.topic.payment-expired}") String paymentExpiredTopic,
                                ObjectMapper objectMapper,
                                OutboxEventRepository outboxEventRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentExpiredTopic = paymentExpiredTopic;
        this.objectMapper = objectMapper;
        this.outboxEventRepository = outboxEventRepository;
    }


    @Transactional
    public int expireDuePayments(int batchSize) {
        OffsetDateTime now = OffsetDateTime.now();
        List<Payment> due = paymentRepository.findDueExpiredForUpdateSkipLocked(now, batchSize);
        if (due.isEmpty()) return 0;

        for (Payment p : due) {
            // 冪等保護：理論上 due query 已過濾，但保險
            if (p.getStatus() != PaymentStatus.INIT) continue;

            p.setStatus(PaymentStatus.EXPIRED);
            p.setExpiredAt(now);

            PaymentExpiredEvent event = new PaymentExpiredEvent(
                    UUID.randomUUID(),
                    paymentExpiredTopic,
                    p.getId(),
                    p.getOrderId(),
                    now
            );

            writeOutbox(
                    "Payment",
                    p.getId(),
                    event.eventType(),
                    event
            );
        }

        // JPA dirty checking + outbox 同一交易提交
        return due.size();
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