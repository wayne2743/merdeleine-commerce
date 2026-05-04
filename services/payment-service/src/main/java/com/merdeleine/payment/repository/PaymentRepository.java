package com.merdeleine.payment.repository;

import com.merdeleine.enums.PaymentStatus;
import com.merdeleine.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByOrderId(UUID orderId);
    Page<Payment> findByOrderId(UUID orderId, Pageable pageable);
    Optional<Payment> findTopByOrderIdOrderByCreatedAtDesc(UUID orderId);

    List<Payment> findByStatus(PaymentStatus status);
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.orderId = :orderId AND p.status = :status")
    List<Payment> findByOrderIdAndStatus(@Param("orderId") UUID orderId,
                                          @Param("status") PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.orderId = :orderId AND p.status = :status")
    Page<Payment> findByOrderIdAndStatus(@Param("orderId") UUID orderId,
                                         @Param("status") PaymentStatus status,
                                         Pageable pageable);

    boolean existsByOrderId(UUID uuid);

    Optional<Payment> findByProviderPaymentId(String providerPaymentId);

    @Query(value = """
        SELECT *
        FROM payment
        WHERE status IN ('INIT', 'PENDING', 'AUTHORIZED', 'FAILED')
          AND expire_at IS NOT NULL
          AND expire_at < :now
        ORDER BY expire_at ASC
        FOR UPDATE SKIP LOCKED
        LIMIT :limit
        """, nativeQuery = true)
    List<Payment> findDueExpiredForUpdateSkipLocked(
            @Param("now") OffsetDateTime now,
            @Param("limit") int limit
    );


}
