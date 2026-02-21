package com.merdeleine.payment.repository;

import com.merdeleine.payment.entity.Payment;
import com.merdeleine.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByOrderId(UUID orderId);

    List<Payment> findByStatus(PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.orderId = :orderId AND p.status = :status")
    List<Payment> findByOrderIdAndStatus(@Param("orderId") UUID orderId, 
                                          @Param("status") PaymentStatus status);

    boolean existsByOrderId(UUID uuid);

    Optional<Payment> findByProviderPaymentId(String providerPaymentId);
}
