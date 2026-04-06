package com.merdeleine.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 由 threshold-service 在收到 PaymentPaidEvent 後發出，
 * 通知 notification-service 發送「銀行轉帳成功，人工審查入帳完成」信件。
 */
public record PaymentPaidNotificationEvent(
        UUID eventId,
        String eventType,          // "payment.paid.notification.v1"
        UUID orderId,
        UUID customerId,
        OffsetDateTime occurredAt
) {
}

