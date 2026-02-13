package com.merdeleine.notification.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.messaging.BatchCreatedNotificationEvent;
import com.merdeleine.notification.entity.NotificationJob;
import com.merdeleine.notification.enums.NotificationChannel;
import com.merdeleine.notification.mapper.NotificationMapper;
import com.merdeleine.notification.repository.NotificationJobRepository;
import com.merdeleine.notification.service.ThresholdMailContextService;
import com.merdeleine.notification.service.ThymeleafMailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;


@Component
public class BatchCreatedNotificationConsumer {

    private final Logger log = LoggerFactory.getLogger(BatchCreatedNotificationConsumer.class);

    private final NotificationJobRepository notificationJobRepository;
    private final ThymeleafMailService thymeleafMailService;
    private final ThymeleafMailService thymeleafThymeleafMailService;
    private final ObjectMapper objectMapper;
    private final ThresholdMailContextService thresholdMailContextService;

    public BatchCreatedNotificationConsumer(NotificationJobRepository notificationJobRepository, ThymeleafMailService thymeleafMailService, ThymeleafMailService thymeleafThymeleafMailService, ObjectMapper objectMapper, ThresholdMailContextService thresholdMailContextService) {

        this.notificationJobRepository = notificationJobRepository;
        this.thymeleafMailService = thymeleafMailService;
        this.thymeleafThymeleafMailService = thymeleafThymeleafMailService;
        this.objectMapper = objectMapper;
        this.thresholdMailContextService = thresholdMailContextService;
    }

    @KafkaListener(
            topics = "${app.kafka.topic.batch-created-notification}",
            groupId = "${app.kafka.consumer.group-id}"
    )
    public void onMessage(
            BatchCreatedNotificationEvent event,
            Acknowledgment ack
    ) throws Exception {
        log.info(
                "[QuotaConfigured] eventId={}, sellWindowId={}, productId={}, batchId={}",
                event.eventId(),
                event.sellWindowId(),
                event.productId(),
                event.batchId()
        );
        NotificationJob saved = notificationJobRepository.save(NotificationMapper.toNotificationJob(event, NotificationChannel.EMAIL,"wayne2347@gmail.com"));

        ThresholdMailContextService.MailContext mailContext = thresholdMailContextService.build(event.productId(), event.sellWindowId());

        thymeleafMailService.sendThresholdReachMail(
                saved.getRecipient(),
                mailContext.productName(),
                mailContext.sellWindowName(),
                event.batchId().toString(),
                event.totalQuantity(),
                event.targetQuantity(),
                saved.getCreatedAt().toString()
        );

        ack.acknowledge();
    }
}
