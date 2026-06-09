package com.merdeleine.notification.messaging;

import com.merdeleine.messaging.OrderAutoCancelledNotificationEvent;
import com.merdeleine.notification.client.ApiGatewayClient;
import com.merdeleine.notification.client.OrderServiceClient;
import com.merdeleine.notification.dto.UserLookupResponse;
import com.merdeleine.notification.entity.NotificationJob;
import com.merdeleine.notification.enums.NotificationChannel;
import com.merdeleine.notification.enums.NotificationStatus;
import com.merdeleine.notification.repository.NotificationJobRepository;
import com.merdeleine.notification.service.ThresholdMailContextService;
import com.merdeleine.notification.service.ThymeleafMailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class OrderAutoCancelledNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderAutoCancelledNotificationConsumer.class);
    private static final String TEMPLATE_KEY = "order-auto-cancelled";
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Taipei"));

    private final ApiGatewayClient apiGatewayClient;
    private final OrderServiceClient orderServiceClient;
    private final ThresholdMailContextService thresholdMailContextService;
    private final NotificationJobRepository notificationJobRepository;
    private final ThymeleafMailService thymeleafMailService;

    public OrderAutoCancelledNotificationConsumer(ApiGatewayClient apiGatewayClient,
                                                   OrderServiceClient orderServiceClient,
                                                   ThresholdMailContextService thresholdMailContextService,
                                                   NotificationJobRepository notificationJobRepository,
                                                   ThymeleafMailService thymeleafMailService) {
        this.apiGatewayClient = apiGatewayClient;
        this.orderServiceClient = orderServiceClient;
        this.thresholdMailContextService = thresholdMailContextService;
        this.notificationJobRepository = notificationJobRepository;
        this.thymeleafMailService = thymeleafMailService;
    }

    @KafkaListener(
            topics = "${app.kafka.topic.order-auto-cancelled-notification-events}",
            groupId = "${app.kafka.consumer.group-id}"
    )
    @Transactional
    public void onMessage(OrderAutoCancelledNotificationEvent event, Acknowledgment ack) {
        log.info("[OrderAutoCancelled] eventId={}, orderId={}, customerId={}, sellWindowId={}",
                event.eventId(), event.orderId(), event.customerId(), event.sellWindowId());

        // 去重：同一 orderId + templateKey 不重複寄
        boolean exists = notificationJobRepository.existsByOrderIdAndTemplateKeyAndChannel(
                event.orderId().toString(),
                TEMPLATE_KEY,
                NotificationChannel.EMAIL.name()
        );
        if (exists) {
            log.info("[OrderAutoCancelled] already notified for orderId={}, skip", event.orderId());
            ack.acknowledge();
            return;
        }

        if (event.customerId() == null) {
            log.warn("[OrderAutoCancelled] customerId is null for orderId={}, skip", event.orderId());
            ack.acknowledge();
            return;
        }

        UserLookupResponse user = apiGatewayClient.getUserByCustomerId(event.customerId());
        if (user == null || user.email() == null || user.email().isBlank()) {
            log.warn("[OrderAutoCancelled] customer not found or email missing for customerId={}, orderId={}, skip",
                    event.customerId(), event.orderId());
            ack.acknowledge();
            return;
        }

        String customerName = user.contactName();
        if (customerName == null || customerName.isBlank()) customerName = user.displayName();
        if (customerName == null || customerName.isBlank()) customerName = "顧客";


        UUID productId = null;
        UUID sellWindowId = event.sellWindowId();
        try {
            var order = orderServiceClient.getOrder(event.orderId());
            if (order != null) {
                productId = order.productId();
                if (sellWindowId == null) {
                    sellWindowId = order.sellWindowId();
                }
            }
        } catch (Exception ex) {
            log.warn("[OrderAutoCancelled] failed to load order for orderId={}, fallback to unknown names", event.orderId(), ex);
        }

        String productName = "(unknown product)";
        String sellWindowName = "(unknown sell window)";
        log.info("[OrderAutoCancelled] load order info: productId={}, sellWindowId={}", productId, sellWindowId);
        if (productId != null && sellWindowId != null) {
            var mailContext = thresholdMailContextService.build(productId, sellWindowId);
            productName = mailContext.productName();
            sellWindowName = mailContext.sellWindowName();
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", event.eventId().toString());
        payload.put("orderId", event.orderId().toString());
        payload.put("orderNo", event.orderNo());
        payload.put("customerId", event.customerId().toString());
        payload.put("customerName", customerName);
        payload.put("customerEmail", user.email());
        payload.put("productId", productId == null ? null : productId.toString());
        payload.put("sellWindowId", sellWindowId == null ? null : sellWindowId.toString());
        payload.put("productName", productName);
        payload.put("sellWindowName", sellWindowName);
        payload.put("cancelReason", event.cancelReason());
        payload.put("occurredAt", FMT.format(event.occurredAt().toInstant()));

        NotificationJob job = new NotificationJob(
                NotificationChannel.EMAIL,
                user.email(),
                TEMPLATE_KEY,
                payload,
                0,
                null
        );
        NotificationJob saved = notificationJobRepository.save(job);

        try {
            thymeleafMailService.sendHtml(
                    saved.getRecipient(),
                    "【merdeleine】訂單已自動取消 - 未達團購門檻",
                    TEMPLATE_KEY,
                    saved.getPayload()
            );

            saved.setStatus(NotificationStatus.SENT);
            saved.setSentAt(OffsetDateTime.now());
            notificationJobRepository.save(saved);

            ack.acknowledge();

        } catch (Exception ex) {
            log.error("[OrderAutoCancelled] send mail failed. jobId={}, orderId={}",
                    saved.getId(), event.orderId(), ex);

            saved.setStatus(NotificationStatus.FAILED);
            saved.setRetryCount(saved.getRetryCount() + 1);
            notificationJobRepository.save(saved);

            throw ex;
        }
    }
}

