package com.merdeleine.order.messaging;

import com.merdeleine.messaging.SellWindowQuotaConfiguredEvent;
import com.merdeleine.order.service.QuotaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;


@Component
public class SellWindowQuotaConfiguredConsumer {

    private final QuotaService quotaService;

    private final Logger log = LoggerFactory.getLogger(SellWindowQuotaConfiguredConsumer.class);

    public SellWindowQuotaConfiguredConsumer(QuotaService quotaService) {
        this.quotaService = quotaService;
    }

    @KafkaListener(
            topics = "${app.kafka.topic.sell-window-quota-configured}",
            groupId = "${app.kafka.consumer.group-id}"
    )
    public void onMessage(
            SellWindowQuotaConfiguredEvent event,
            Acknowledgment ack
    ) {
        log.info(
                "[QuotaConfigured] eventId={}, sellWindowId={}, productId={}",
                event.eventId(),
                event.sellWindowId(),
                event.productId()
        );

        if(quotaService.isProcessed(event.sellWindowId(), event.productId())) {
            log.info(
                    "[QuotaConfigured] duplicate event detected, skipping processing for eventId={}, sellWindowId={}, productId={}",
                    event.eventId(),
                    event.sellWindowId(),
                    event.productId()
            );
            ack.acknowledge(); // ✅ 已處理過也要 ack
            return;
        }

        try {
            quotaService.apply(event);
            ack.acknowledge(); // ✅ 成功才 ack
        } catch (Exception e) {
            log.error(
                    "[QuotaConfigured] failed, eventId={}",
                    event.eventId(),
                    e
            );
            // ❌ 不 ack → Kafka 會 retry
            throw e;
        }
    }
}
