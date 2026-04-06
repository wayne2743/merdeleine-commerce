package com.merdeleine.notification.messaging;

import com.merdeleine.notification.client.ApiGatewayClient;
import com.merdeleine.notification.dto.UserLookupResponse;
import com.merdeleine.messaging.PaymentCreatedEvent;
import com.merdeleine.notification.entity.NotificationJob;
import com.merdeleine.notification.enums.NotificationChannel;
import com.merdeleine.notification.enums.NotificationStatus;

import com.merdeleine.notification.mapper.NotificationMapper;
import com.merdeleine.notification.repository.NotificationJobRepository;
import com.merdeleine.notification.service.ThymeleafMailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentCreatedConsumer {

    private final Logger log = LoggerFactory.getLogger(PaymentCreatedConsumer.class);

    private final ApiGatewayClient apiGatewayClient;
    private final NotificationJobRepository notificationJobRepository;
    private final ThymeleafMailService thymeleafMailService;

    public PaymentCreatedConsumer(ApiGatewayClient apiGatewayClient,
                                  NotificationJobRepository notificationJobRepository,
                                  ThymeleafMailService thymeleafMailService) {
        this.apiGatewayClient = apiGatewayClient;
        this.notificationJobRepository = notificationJobRepository;
        this.thymeleafMailService = thymeleafMailService;
    }

    @KafkaListener(
            topics = "${app.kafka.topic.payment-created-events}",
            groupId = "${app.kafka.consumer.group-id}"
    )
    @Transactional
    public void onMessage(PaymentCreatedEvent event, Acknowledgment ack) {
        log.info("[PaymentCreated] eventId={}, orderId={}, paymentId={}, providerPaymentId={}, customerId={}, amount={}, provider={}, expireAt={}",
                event.eventId(),
                event.orderId(),
                event.paymentId(),
                event.providerPaymentId(),
                event.customerId(),
                event.totalAmount(),
                event.paymentProvider(),
                event.expireAt()
        );

        // 0) 去重：同一 paymentId 的信不要寄兩次（利用 payload jsonb 查詢）
        boolean exists = notificationJobRepository.existsByPaymentIdAndTemplateKeyAndChannel(
                event.paymentId().toString(),
                NotificationMapper.TEMPLATE_KEY,
                NotificationChannel.EMAIL.name()
        );

        if(event.customerId() == null){
            log.warn("[PaymentCreated] customerId is null for paymentId={}, eventId={}. Skipping notification.",
                    event.paymentId(), event.eventId());
            ack.acknowledge();
            return;
        }

        if (exists) {
            log.info("[PaymentCreated] duplicated paymentId={}, templateKey={} -> skip",
                    event.paymentId(), NotificationMapper.TEMPLATE_KEY);
            ack.acknowledge();
            return;
        }

        // 1) 建立 job（REQUESTED）
        UserLookupResponse user = apiGatewayClient.getUserByCustomerId(event.customerId());
        if (user == null || user.email() == null || user.email().isBlank()) {
            throw new IllegalStateException("Customer email not found for customerId=" + event.customerId());
        }

        String customerName = user.contactName();
        if (customerName == null || customerName.isBlank()) {
            customerName = user.displayName();
        }
        if (customerName == null || customerName.isBlank()) {
            customerName = "Customer";
        }

        NotificationJob job = NotificationMapper.toJob(event, user.email(), customerName);
        NotificationJob saved = notificationJobRepository.save(job);

        try {
            // 2) 寄信（subject 可依 provider / expireAt 客製）
            String subject = "【merdeleine】付款資訊已建立，請於期限內完成付款";

            thymeleafMailService.sendHtml(
                    saved.getRecipient(),
                    subject,
                    saved.getTemplateKey(),
                    saved.getPayload()
            );

            // 3) 成功：SENT + sentAt
            saved.setStatus(NotificationStatus.SENT);
            saved.setSentAt(java.time.OffsetDateTime.now());
            notificationJobRepository.save(saved);

            // 4) ack
            ack.acknowledge();

        } catch (Exception ex) {
            log.error("[PaymentCreated] send mail failed. jobId={}, eventId={}, paymentId={}",
                    saved.getId(), event.eventId(), event.paymentId(), ex);

            saved.setStatus(NotificationStatus.FAILED);
            saved.setRetryCount(saved.getRetryCount() + 1);
            notificationJobRepository.save(saved);

            // 交給 Kafka 的 retry/DLQ（你若還沒配，至少先讓它重試）
            throw ex;
        }
    }
}