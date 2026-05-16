package com.merdeleine.order.messaging;

import com.merdeleine.messaging.SellWindowClosedEvent;
import com.merdeleine.order.service.OrderService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class SellWindowClosedConsumer {

    private final OrderService orderService;

    private final Logger log = LoggerFactory.getLogger(SellWindowClosedConsumer.class);

    public SellWindowClosedConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(
            topics = "${merdeleine.kafka.topics.sell-window-closed}",
            groupId = "${app.kafka.consumer.group-id}"
    )
    @Transactional
    public void onMessage(
            SellWindowClosedEvent event,
            Acknowledgment ack
    ) {
        log.info(
                "[SellWindowClosed] eventId={}, sellWindowId={}, occurredAt={}",
                event.eventId(),
                event.sellWindowId(),
                event.occurredAt()
        );

        // 批量取消該 sellWindowId 下的所有 RESERVED 和 PAYMENT_REQUESTED 訂單
        orderService.cancelBySellWindowIdAutomatically(event.sellWindowId());

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

