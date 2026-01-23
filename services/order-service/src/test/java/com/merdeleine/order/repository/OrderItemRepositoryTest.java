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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class OrderItemRepositoryTest {

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
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Order testOrder;
    private UUID productId;
    private UUID variantId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        variantId = UUID.randomUUID();

        testOrder = new Order();
        testOrder.setId(UUID.randomUUID());
        testOrder.setOrderNo("ORD-001");
        testOrder.setCustomerId(UUID.randomUUID());
        testOrder.setStatus(OrderStatus.PENDING_PAYMENT);
        testOrder.setTotalAmountCents(10000);
        testOrder.setCurrency("TWD");
        entityManager.persistAndFlush(testOrder);
    }

    @Test
    void testSaveOrderItem() {
        OrderItem item = new OrderItem();
        item.setId(UUID.randomUUID());
        item.setOrder(testOrder);
        item.setProductId(productId);
        item.setVariantId(variantId);
        item.setQuantity(2);
        item.setUnitPriceCents(5000);
        item.setSubtotalCents(10000);
        
        OrderItem saved = orderItemRepository.saveAndFlush(item);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getProductId()).isEqualTo(productId);
        assertThat(saved.getQuantity()).isEqualTo(2);
    }

    @Test
    void testFindByOrderId() {
        OrderItem item1 = createOrderItem(testOrder, productId, variantId, 2);
        OrderItem item2 = createOrderItem(testOrder, UUID.randomUUID(), UUID.randomUUID(), 1);
        
        entityManager.persistAndFlush(item1);
        entityManager.persistAndFlush(item2);
        
        List<OrderItem> items = orderItemRepository.findByOrderId(testOrder.getId());
        
        assertThat(items).hasSize(2);
    }

    @Test
    void testFindByProductId() {
        OrderItem item1 = createOrderItem(testOrder, productId, variantId, 2);
        Order order2 = createTestOrder("ORD-002");
        OrderItem item2 = createOrderItem(order2, productId, UUID.randomUUID(), 1);
        
        entityManager.persistAndFlush(item1);
        entityManager.persistAndFlush(item2);
        
        List<OrderItem> items = orderItemRepository.findByProductId(productId);
        
        assertThat(items).hasSize(2);
        assertThat(items).extracting(OrderItem::getProductId).containsOnly(productId);
    }

    @Test
    void testFindByVariantId() {
        OrderItem item = createOrderItem(testOrder, productId, variantId, 2);
        entityManager.persistAndFlush(item);
        
        List<OrderItem> items = orderItemRepository.findByVariantId(variantId);
        
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getVariantId()).isEqualTo(variantId);
    }

    @Test
    void testDeleteOrderItem() {
        OrderItem item = createOrderItem(testOrder, productId, variantId, 2);
        OrderItem saved = entityManager.persistAndFlush(item);
        UUID itemId = saved.getId();
        
        orderItemRepository.delete(saved);
        entityManager.flush();
        
        assertThat(orderItemRepository.findById(itemId)).isEmpty();
    }

    private OrderItem createOrderItem(Order order, UUID productId, UUID variantId, int quantity) {
        OrderItem item = new OrderItem();
        item.setId(UUID.randomUUID());
        item.setOrder(order);
        item.setProductId(productId);
        item.setVariantId(variantId);
        item.setQuantity(quantity);
        item.setUnitPriceCents(5000);
        item.setSubtotalCents(quantity * 5000);
        return item;
    }

    private Order createTestOrder(String orderNo) {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setOrderNo(orderNo);
        order.setCustomerId(UUID.randomUUID());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setTotalAmountCents(10000);
        order.setCurrency("TWD");
        return entityManager.persistAndFlush(order);
    }
}
