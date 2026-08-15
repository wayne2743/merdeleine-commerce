package com.merdeleine.payment.repository;

import com.merdeleine.payment.entity.PaymentRefund;
import com.merdeleine.payment.enums.PaymentRefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, UUID> {

    Optional<PaymentRefund> findByPaymentIdAndIdempotencyKey(UUID paymentId, String idempotencyKey);

    boolean existsByPaymentIdAndStatus(UUID paymentId, PaymentRefundStatus status);

    @Query(value = """
            SELECT COALESCE(SUM(amount_cents), 0)
            FROM payment_refund
            WHERE payment_id = :paymentId AND status = 'SUCCEEDED'
            """, nativeQuery = true)
    long sumSucceededAmountCents(@Param("paymentId") UUID paymentId);
}
