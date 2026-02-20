package com.merdeleine.notification.mapper;

import com.merdeleine.messaging.BatchCreatedNotificationEvent;
import com.merdeleine.messaging.PaymentCreatedEvent;
import com.merdeleine.notification.entity.NotificationJob;
import com.merdeleine.notification.enums.NotificationChannel;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class NotificationMapper {
    private static final DateTimeFormatter EXPIRE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Taipei"));

    public static final String TEMPLATE_KEY = "payment-created";


    public static NotificationJob toNotificationJob(BatchCreatedNotificationEvent event, NotificationChannel channel, String recipient) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", event);
        payload.put("productId", event.productId());
        payload.put("sellWindowId", event.sellWindowId());
        payload.put("batchId", event.batchId());
        return new NotificationJob(
                channel,
                recipient,
                "batch.created.notification.v1",
                payload,
                3,
                OffsetDateTime.now()
        );
    }


    public static NotificationJob toJob(PaymentCreatedEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", event.eventId().toString());
        payload.put("eventType", event.eventType());
        payload.put("orderId", event.orderId().toString());
        payload.put("paymentId", event.paymentId().toString());
        payload.put("customerName", event.customerName());
        payload.put("customerEmail", event.customerEmail());
        payload.put("totalAmount", event.totalAmount());
        payload.put("paymentProvider", event.paymentProvider().name());
        payload.put("expireAtText", EXPIRE_FMT.format(event.expireAt().toInstant()));

        // 你之後若有付款連結可加：
        // payload.put("payUrl", "...");

        return new NotificationJob(
                NotificationChannel.EMAIL,
                event.customerEmail(),
                TEMPLATE_KEY,
                payload,
                0,
                null
        );
    }
}
