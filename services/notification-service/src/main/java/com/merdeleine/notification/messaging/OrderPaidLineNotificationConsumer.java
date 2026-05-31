package com.merdeleine.notification.messaging;

import com.merdeleine.enums.OrderStatus;
import com.merdeleine.messaging.PaymentPaidEvent;
import com.merdeleine.notification.entity.NotificationJob;
import com.merdeleine.notification.entity.LineUser;
import com.merdeleine.notification.enums.NotificationChannel;
import com.merdeleine.notification.enums.NotificationStatus;
import com.merdeleine.notification.repository.LineUserRepository;
import com.merdeleine.notification.repository.NotificationJobRepository;
import com.merdeleine.notification.service.LineMessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

@Component
public class OrderPaidLineNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderPaidLineNotificationConsumer.class);
    private static final String TEMPLATE_KEY = "order-paid-line";

    private final LineUserRepository lineUserRepository;
    private final LineMessagingService lineMessagingService;
    private final NotificationJobRepository notificationJobRepository;

    public OrderPaidLineNotificationConsumer(LineUserRepository lineUserRepository,
                                             LineMessagingService lineMessagingService,
                                             NotificationJobRepository notificationJobRepository) {
        this.lineUserRepository = lineUserRepository;
        this.lineMessagingService = lineMessagingService;
        this.notificationJobRepository = notificationJobRepository;
    }

    @KafkaListener(
            topics = "${app.kafka.topic.payment-paid-events}",
            groupId = "${app.kafka.consumer.group-id}-line"
    )
    @Transactional
    public void onMessage(PaymentPaidEvent event, Acknowledgment ack) {
        log.info("[OrderPaidLine] eventId={}, orderId={}, status={}",
                event.eventId(), event.orderId(), event.orderStatus());

        if (event.orderStatus() != OrderStatus.PAID) {
            ack.acknowledge();
            return;
        }

        if (event.customerId() == null) {
            log.warn("[OrderPaidLine] customerId is null for orderId={}, skip", event.orderId());
            ack.acknowledge();
            return;
        }

        // 去重
        boolean exists = notificationJobRepository.existsByOrderIdAndTemplateKeyAndChannel(
                event.orderId().toString(), TEMPLATE_KEY, NotificationChannel.LINE.name());
        if (exists) {
            log.info("[OrderPaidLine] already sent for orderId={}, skip", event.orderId());
            ack.acknowledge();
            return;
        }

        LineUser lineUser = lineUserRepository.findByMemberIdAndFollowedTrue(event.customerId())
                .orElse(null);
        if (lineUser == null) {
            log.info("[OrderPaidLine] no LINE binding for customerId={}, skip", event.customerId());
            ack.acknowledge();
            return;
        }

        String message = String.format("【merdeleine】您的訂單 %s 已完成付款，感謝您的支持！", event.orderId());

        NotificationJob job = new NotificationJob(
                NotificationChannel.LINE,
                lineUser.getUserId(),
                TEMPLATE_KEY,
                Map.of("orderId", event.orderId().toString(),
                       "customerId", event.customerId().toString(),
                       "lineUserId", lineUser.getUserId()),
                0,
                null
        );
        NotificationJob saved = notificationJobRepository.save(job);

        try {
            lineMessagingService.pushMessage(lineUser.getUserId(), message);
            saved.setStatus(NotificationStatus.SENT);
            saved.setSentAt(OffsetDateTime.now());
            notificationJobRepository.save(saved);
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("[OrderPaidLine] push failed. jobId={}, orderId={}", saved.getId(), event.orderId(), ex);
            saved.setStatus(NotificationStatus.FAILED);
            saved.setRetryCount(saved.getRetryCount() + 1);
            notificationJobRepository.save(saved);
            throw ex;
        }
    }
}
