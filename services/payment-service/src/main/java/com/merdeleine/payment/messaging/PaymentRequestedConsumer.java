package com.merdeleine.payment.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.messaging.PaymentCreatedEvent;
import com.merdeleine.messaging.PaymentRequestedEvent;
import com.merdeleine.payment.entity.OutboxEvent;
import com.merdeleine.payment.entity.Payment;
import com.merdeleine.payment.enums.OutboxEventStatus;
import com.merdeleine.enums.PaymentStatus;
import com.merdeleine.payment.repository.OutboxEventRepository;
import com.merdeleine.payment.repository.PaymentRepository;
import com.merdeleine.payment.utils.MerchantTradeNoGenerator;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentRequestedConsumer {

    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;
    private final String paymentCreatedTopic; // 你可以直接在 @KafkaListener 用 property，不一定要注入這個 field

    private final Logger log = LoggerFactory.getLogger(PaymentRequestedConsumer.class);

    // 你原本注入了 topic，但 @KafkaListener 直接用 property 就夠了
    public PaymentRequestedConsumer(
            PaymentRepository paymentRepository,
            ObjectMapper objectMapper,
            OutboxEventRepository outboxEventRepository,
            @Value("${app.kafka.topic.payment-created-events}") String paymentCreatedTopic

            ) {
        this.paymentRepository = paymentRepository;
        this.objectMapper = objectMapper;
        this.outboxEventRepository = outboxEventRepository;
        this.paymentCreatedTopic = paymentCreatedTopic;
    }

    @KafkaListener(
            topics = "${app.kafka.topic.payment-requested-events}",
            groupId = "${app.kafka.consumer.group-id}"
    )
    @Transactional
    public void onMessage(PaymentRequestedEvent event, Acknowledgment ack) {

        log.info("[PaymentRequested] eventId={}, orderId={}",
                event.eventId(), event.orderId());

        // 1) idempotent：同一個 orderId 若已建立 payment，就直接跳過
        if (paymentRepository.existsByOrderId(event.orderId())) {
            log.info("[PaymentRequested] orderId={} payment already exists, skip", event.orderId());
            ack.acknowledge();
            return;
        }

        // 2) 建立 payment（請依你的 PaymentRequestedEvent 欄位調整金額/幣別）
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(event.orderId());
        payment.setStatus(PaymentStatus.INIT);
        payment.setAmountCents(event.totalAmount());
        payment.setCurrency(event.currency());
        payment.setProvider(event.provider());
        payment.setProviderPaymentId(MerchantTradeNoGenerator.generate());

        paymentRepository.save(payment);

        // 3) 寫 Outbox：PaymentCreated / PaymentPending（事件名稱你可自行調整）
        // payload 可以放 paymentId/orderId/amount/currency/eventId 等資訊
        writeOutbox(
                "PAYMENT",
                payment.getId(),
                paymentCreatedTopic,
                new PaymentCreatedEvent(
                        event.eventId(),
                        paymentCreatedTopic,
                        payment.getOrderId(),
                        payment.getId(),
                        payment.getProviderPaymentId(),
                        event.customerEmail(),
                        event.customerName(),
                        payment.getAmountCents(),
                        event.provider(),
                        event.expiresAt()
                )
        );

        log.info("[PaymentRequested] created paymentId={} for orderId={}",
                payment.getId(), payment.getOrderId());

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