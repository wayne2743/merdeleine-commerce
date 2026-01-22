package com.merdeleine.catalog.repository;

import com.merdeleine.catalog.entity.PaymentTxn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentTxnRepository extends JpaRepository<PaymentTxn, UUID> {

    List<PaymentTxn> findByPaymentId(UUID paymentId);
}
