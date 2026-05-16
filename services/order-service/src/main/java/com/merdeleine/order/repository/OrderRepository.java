package com.merdeleine.order.repository;

import com.merdeleine.order.entity.Order;
import com.merdeleine.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderNo(String orderNo);

    List<Order> findByCustomerId(UUID customerId);

    List<Order> findByCustomerIdAndStatusNot(UUID customerId, OrderStatus status);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findBySellWindowId(UUID sellWindowId);

    List<Order> findBySellWindowIdAndStatus(UUID sellWindowId, OrderStatus status);

    List<Order> findBySellWindowIdAndStatusNot(UUID sellWindowId, OrderStatus status);

    List<Order> findBySellWindowIdAndStatusIn(UUID sellWindowId, List<OrderStatus> statuses);

    @Query("SELECT o FROM Order o WHERE o.customerId = :customerId AND o.status = :status")
    List<Order> findByCustomerIdAndStatus(@Param("customerId") UUID customerId,
                                          @Param("status") OrderStatus status);

    @Query("""
        SELECT o FROM Order o
        JOIN FETCH o.item
        WHERE o.customerId = :customerId
          AND o.sellWindowId = :sellWindowId
          AND o.status = :status
    """)
    Optional<Order> findActiveOrderByCustomerAndSellWindow(
            @Param("customerId") UUID customerId,
            @Param("sellWindowId") UUID sellWindowId,
            @Param("status") OrderStatus status
    );
}
