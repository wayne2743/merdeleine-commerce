package com.merdeleine.notification.mapper;

import com.merdeleine.messaging.BatchCreatedNotificationEvent;
import com.merdeleine.notification.entity.NotificationJob;
import com.merdeleine.notification.enums.NotificationChannel;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

public class NotificationMapper {

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
}
