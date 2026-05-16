package com.merdeleine.order.repository;

import com.merdeleine.order.entity.OrderDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderDeliveryRepository extends JpaRepository<OrderDelivery, UUID> {
    Optional<OrderDelivery> findByOrderId(UUID orderId);
}

