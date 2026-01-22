package com.merdeleine.order.repository;

import com.merdeleine.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrderId(UUID orderId);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.productId = :productId")
    List<OrderItem> findByProductId(@Param("productId") UUID productId);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.variantId = :variantId")
    List<OrderItem> findByVariantId(@Param("variantId") UUID variantId);
}
