package com.merdeleine.payment.mapper;

import com.merdeleine.messaging.PaymentCompletedEvent;
import com.merdeleine.payment.entity.Payment;

import java.util.UUID;

public class PaymentMapper {
    public static PaymentCompletedEvent toPaymentEvent(Payment payment,
                                                       String eventType) {
        return new PaymentCompletedEvent(
                UUID.randomUUID(),
                eventType,
                payment.getId(),
                payment.getOrderId(),
                payment.getStatus()
        );
    }
}
