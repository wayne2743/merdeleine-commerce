package com.merdeleine.order.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.messaging.BatchConfirmEvent;
import com.merdeleine.messaging.PaymentRequestedEvent;
import com.merdeleine.order.entity.Order;
import com.merdeleine.order.entity.OutboxEvent;
import com.merdeleine.order.enums.OutboxEventStatus;
import com.merdeleine.order.repository.OrderRepository;
import com.merdeleine.order.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;


@Component
public class BatchConfirmConsumer {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;
    private final String paymentRequestedTopic;

    private final Logger log = LoggerFactory.getLogger(BatchConfirmConsumer.class);

    public BatchConfirmConsumer(OrderRepository orderRepository,
                                ObjectMapper objectMapper,
                                OutboxEventRepository outboxEventRepository,
                                @Value("${app.kafka.topic.payment-requested-events}") String paymentRequestedTopic) {
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
        this.outboxEventRepository = outboxEventRepository;
        this.paymentRequestedTopic = paymentRequestedTopic;
    }

    @KafkaListener(
            topics = "${app.kafka.topic.batch-confirm-events}",
            groupId = "${app.kafka.consumer.group-id}"
    )
    public void onMessage(
            BatchConfirmEvent event,
            Acknowledgment ack
    ) {
        log.info(
                "[QuotaConfigured] eventId={}, sellWindowId={}, productId={}, batchId={}",
                event.eventId(),
                event.sellWindowId(),
                event.productId(),
                event.batchId()
        );

        List<Order> orders = orderRepository.findBySellWindowId(event.sellWindowId());

        orders.stream().forEach(order -> {
            writeOutbox(
                    "Order",
                    order.getId(),
                    paymentRequestedTopic,
                    new PaymentRequestedEvent(
                            UUID.randomUUID(),
                            paymentRequestedTopic,
                            order.getId(),
                            order.getTotalAmountCents(),
                            order.getCurrency()
                    )
            );

        });
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
