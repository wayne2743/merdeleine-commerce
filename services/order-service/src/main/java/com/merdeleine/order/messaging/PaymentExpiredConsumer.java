package com.merdeleine.order.messaging;

import com.merdeleine.enums.OrderStatus;
import com.merdeleine.messaging.PaymentExpiredEvent;
import com.merdeleine.order.entity.Order;
import com.merdeleine.order.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class PaymentExpiredConsumer {

    private final OrderRepository orderRepository;

    private final Logger log = LoggerFactory.getLogger(PaymentExpiredConsumer.class);

    public PaymentExpiredConsumer(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @KafkaListener(
            topics = "${app.kafka.topic.payment-expired-events}",
            groupId = "${app.kafka.consumer.group-id}"
    )
    @Transactional
    public void onMessage(
            PaymentExpiredEvent event,
            Acknowledgment ack
    ) {
        log.info(
                "[PaymentExpired] eventId={}, orderId={}, paymentId={}, expiredAt={}",
                event.eventId(),
                event.orderId(),
                event.paymentId(),
                event.expiredAt()
        );

        Order order = orderRepository.findById(event.orderId()).orElseThrow(
                () -> new RuntimeException("Order not found for orderId: " + event.orderId())
        );

        // 仅当订单状态为 PAYMENT_REQUESTED 时才更新为 EXPIRED
        if (order.getStatus() == OrderStatus.PAYMENT_REQUESTED) {
            order.setStatus(OrderStatus.EXPIRED);
            log.info("[PaymentExpired] Order status updated to EXPIRED. orderId={}", event.orderId());
        } else {
            log.warn("[PaymentExpired] Order status is not PAYMENT_REQUESTED, skipping. " +
                    "orderId={}, currentStatus={}", event.orderId(), order.getStatus());
        }

        // ✅ 重點：等交易「真的 commit」成功後才 ack
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    ack.acknowledge();
                }
            });
        } else {
            // 理論上在 @Transactional 內會 active；保底用
            ack.acknowledge();
        }
    }
}


