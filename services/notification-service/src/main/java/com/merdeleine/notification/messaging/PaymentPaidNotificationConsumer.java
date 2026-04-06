package com.merdeleine.notification.messaging;

import com.merdeleine.messaging.PaymentPaidNotificationEvent;
import com.merdeleine.notification.client.ApiGatewayClient;
import com.merdeleine.notification.dto.UserLookupResponse;
import com.merdeleine.notification.entity.NotificationJob;
import com.merdeleine.notification.enums.NotificationChannel;
import com.merdeleine.notification.enums.NotificationStatus;
import com.merdeleine.notification.repository.NotificationJobRepository;
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

@Component
public class PaymentPaidNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentPaidNotificationConsumer.class);
    private static final String TEMPLATE_KEY = "bank-transfer-approved";
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Taipei"));

    private final ApiGatewayClient apiGatewayClient;
    private final NotificationJobRepository notificationJobRepository;
    private final ThymeleafMailService thymeleafMailService;

    public PaymentPaidNotificationConsumer(ApiGatewayClient apiGatewayClient,
                                           NotificationJobRepository notificationJobRepository,
                                           ThymeleafMailService thymeleafMailService) {
        this.apiGatewayClient = apiGatewayClient;
        this.notificationJobRepository = notificationJobRepository;
        this.thymeleafMailService = thymeleafMailService;
    }

    @KafkaListener(
            topics = "${app.kafka.topic.payment-paid-notification-events}",
            groupId = "${app.kafka.consumer.group-id}"
    )
    @Transactional
    public void onMessage(PaymentPaidNotificationEvent event, Acknowledgment ack) {
        log.info("[BankTransferApproved] eventId={}, orderId={}, customerId={}",
                event.eventId(), event.orderId(), event.customerId());

        // 去重：同一 orderId + templateKey 不重複寄
        boolean exists = notificationJobRepository.existsByOrderIdAndTemplateKeyAndChannel(
                event.orderId().toString(),
                TEMPLATE_KEY,
                NotificationChannel.EMAIL.name()
        );
        if (exists) {
            log.info("[BankTransferApproved] already notified for orderId={}, skip", event.orderId());
            ack.acknowledge();
            return;
        }

        if (event.customerId() == null) {
            log.warn("[BankTransferApproved] customerId is null for orderId={}, skip", event.orderId());
            ack.acknowledge();
            return;
        }

        UserLookupResponse user = apiGatewayClient.getUserByCustomerId(event.customerId());
        if (user == null || user.email() == null || user.email().isBlank()) {
            throw new IllegalStateException("Customer email not found for customerId=" + event.customerId());
        }

        String customerName = user.contactName();
        if (customerName == null || customerName.isBlank()) customerName = user.displayName();
        if (customerName == null || customerName.isBlank()) customerName = "顧客";

        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", event.eventId().toString());
        payload.put("orderId", event.orderId().toString());
        payload.put("customerId", event.customerId().toString());
        payload.put("customerName", customerName);
        payload.put("customerEmail", user.email());
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
                    "【merdeleine】銀行轉帳成功 - 已人工審查入帳完成",
                    TEMPLATE_KEY,
                    saved.getPayload()
            );

            saved.setStatus(NotificationStatus.SENT);
            saved.setSentAt(OffsetDateTime.now());
            notificationJobRepository.save(saved);

            ack.acknowledge();

        } catch (Exception ex) {
            log.error("[BankTransferApproved] send mail failed. jobId={}, orderId={}",
                    saved.getId(), event.orderId(), ex);

            saved.setStatus(NotificationStatus.FAILED);
            saved.setRetryCount(saved.getRetryCount() + 1);
            notificationJobRepository.save(saved);

            throw ex;
        }
    }
}

