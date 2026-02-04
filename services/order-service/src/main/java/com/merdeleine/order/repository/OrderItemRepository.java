package com.merdeleine.order.repository;

import com.merdeleine.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    List<OrderItem> findByProductId(UUID productId);

    List<OrderItem> findByOrderId(UUID id);
}
