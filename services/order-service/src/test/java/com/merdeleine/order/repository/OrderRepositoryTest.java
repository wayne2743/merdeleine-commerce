package com.merdeleine.order.repository;

import com.merdeleine.order.entity.Order;
import com.merdeleine.order.entity.OrderItem;
import com.merdeleine.order.enums.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class OrderRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("order_db")
            .withUsername("merdeleine")
            .withPassword("merdeleine_pw");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private UUID customerId;
    private UUID sellWindowId;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        sellWindowId = UUID.randomUUID();
        
        testOrder = new Order();
        testOrder.setId(UUID.randomUUID());
        testOrder.setOrderNo("ORD-001");
        testOrder.setCustomerId(customerId);
        testOrder.setSellWindowId(sellWindowId);
        testOrder.setStatus(OrderStatus.PENDING_PAYMENT);
        testOrder.setTotalAmountCents(10000);
        testOrder.setCurrency("TWD");
        testOrder.setContactName("Test User");
        testOrder.setContactPhone("0912345678");
        testOrder.setContactEmail("test@example.com");
        testOrder.setShippingAddress("Test Address");
    }

    @Test
    void testSaveOrder() {
        Order saved = orderRepository.saveAndFlush(testOrder);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOrderNo()).isEqualTo("ORD-001");
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void testFindByOrderNo() {
        entityManager.persistAndFlush(testOrder);
        
        Optional<Order> found = orderRepository.findByOrderNo("ORD-001");
        
        assertThat(found).isPresent();
        assertThat(found.get().getOrderNo()).isEqualTo("ORD-001");
    }

    @Test
    void testFindByCustomerId() {
        entityManager.persistAndFlush(testOrder);
        
        Order order2 = new Order();
        order2.setOrderNo("ORD-002");
        order2.setCustomerId(customerId);
        order2.setStatus(OrderStatus.PAID);
        order2.setTotalAmountCents(20000);
        order2.setCurrency("TWD");
        order2.setId(UUID.randomUUID());
        entityManager.persistAndFlush(order2);
        
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        
        assertThat(orders).hasSize(2);
        assertThat(orders).extracting(Order::getOrderNo).containsExactlyInAnyOrder("ORD-001", "ORD-002");
    }

    @Test
    void testFindByStatus() {
        entityManager.persistAndFlush(testOrder);
        
        Order paidOrder = new Order();
        paidOrder.setId(UUID.randomUUID());
        paidOrder.setOrderNo("ORD-003");
        paidOrder.setCustomerId(UUID.randomUUID());
        paidOrder.setStatus(OrderStatus.PAID);
        paidOrder.setTotalAmountCents(15000);
        paidOrder.setCurrency("TWD");
        entityManager.persistAndFlush(paidOrder);
        
        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING_PAYMENT);
        
        assertThat(pendingOrders).hasSize(1);
        assertThat(pendingOrders.get(0).getOrderNo()).isEqualTo("ORD-001");
    }

    @Test
    void testFindBySellWindowId() {
        entityManager.persistAndFlush(testOrder);
        
        List<Order> orders = orderRepository.findBySellWindowId(sellWindowId);
        
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getSellWindowId()).isEqualTo(sellWindowId);
    }

    @Test
    void testFindByCustomerIdAndStatus() {
        entityManager.persistAndFlush(testOrder);
        
        List<Order> orders = orderRepository.findByCustomerIdAndStatus(customerId, OrderStatus.PENDING_PAYMENT);
        
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void testOrderWithItems() {
        Order saved = entityManager.persistAndFlush(testOrder);
        
        OrderItem item1 = new OrderItem();
        item1.setOrder(saved);
        item1.setProductId(UUID.randomUUID());
        item1.setVariantId(UUID.randomUUID());
        item1.setQuantity(2);
        item1.setUnitPriceCents(5000);
        item1.setSubtotalCents(10000);
        item1.setId(UUID.randomUUID());

        OrderItem item2 = new OrderItem();
        item2.setOrder(saved);
        item2.setProductId(UUID.randomUUID());
        item2.setVariantId(UUID.randomUUID());
        item2.setQuantity(1);
        item2.setUnitPriceCents(3000);
        item2.setSubtotalCents(3000);
        item2.setId(UUID.randomUUID());
        
        entityManager.persistAndFlush(item1);
        entityManager.persistAndFlush(item2);
        entityManager.clear();
        
        Order found = orderRepository.findById(saved.getId()).orElseThrow();
        List<OrderItem> items = orderItemRepository.findByOrderId(found.getId());

        assertThat(items).hasSize(2);
        assertThat(items).extracting(OrderItem::getQuantity).containsExactlyInAnyOrder(2, 1);
    }

    @Test
    void testUpdateOrderStatus() {
        Order saved = entityManager.persistAndFlush(testOrder);
        saved.setStatus(OrderStatus.PAID);
        Order updated = orderRepository.saveAndFlush(saved);
        
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(updated.getUpdatedAt()).isAfter(saved.getCreatedAt());
    }

    @Test
    void testDeleteOrder() {
        Order saved = entityManager.persistAndFlush(testOrder);
        UUID orderId = saved.getId();
        
        orderRepository.delete(saved);
        entityManager.flush();
        
        Optional<Order> deleted = orderRepository.findById(orderId);
        assertThat(deleted).isEmpty();
    }
}
