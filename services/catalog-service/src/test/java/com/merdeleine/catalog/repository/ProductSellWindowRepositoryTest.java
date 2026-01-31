package com.merdeleine.catalog.repository;


import com.merdeleine.catalog.entity.Product;
import com.merdeleine.catalog.entity.ProductSellWindow;
import com.merdeleine.catalog.entity.SellWindow;
import com.merdeleine.catalog.enums.ProductStatus;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ProductSellWindowRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("catalog_db")
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
    private ProductSellWindowRepository productSellWindowRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SellWindowRepository sellWindowRepository;

    private Product testProduct;
    private SellWindow testSellWindow;
    private UUID productId;
    private UUID sellWindowId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        sellWindowId = UUID.randomUUID();
        
        testProduct = new Product();
        testProduct.setId(productId);
        testProduct.setName("Test Product");
        testProduct.setStatus(ProductStatus.ACTIVE);
        entityManager.persistAndFlush(testProduct);
        
        testSellWindow = new SellWindow();
        testSellWindow.setId(sellWindowId);
        testSellWindow.setName("Spring 2024");
        testSellWindow.setStartAt(OffsetDateTime.now());
        testSellWindow.setEndAt(OffsetDateTime.now().plusMonths(3));
        testSellWindow.setTimezone("Asia/Taipei");
        entityManager.persistAndFlush(testSellWindow);
    }

    @Test
    void testSaveProductSellWindow() {
        ProductSellWindow psw = new ProductSellWindow();
        psw.setId(UUID.randomUUID());
        psw.setProduct(testProduct);
        psw.setSellWindow(testSellWindow);
        psw.setMinTotalQty(100);
        psw.setMaxTotalQty(500);
        psw.setLeadDays(7);
        psw.setShipDays(14);
        psw.setClosed(true);
        
        ProductSellWindow saved = productSellWindowRepository.saveAndFlush(psw);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getMinTotalQty()).isEqualTo(100);
        assertThat(saved.getMaxTotalQty()).isEqualTo(500);
        assertThat(saved.getLeadDays()).isEqualTo(7);
        assertThat(saved.getShipDays()).isEqualTo(14);
        assertThat(saved.isClosed()).isTrue();
    }

    @Test
    void testFindByProductId() {
        ProductSellWindow psw1 = createProductSellWindow(testProduct, testSellWindow, 100);
        SellWindow sellWindow2 = createSellWindow("Summer 2024");
        ProductSellWindow psw2 = createProductSellWindow(testProduct, sellWindow2, 200);
        
        entityManager.persistAndFlush(psw1);
        entityManager.persistAndFlush(psw2);
        
        List<ProductSellWindow> psws = productSellWindowRepository.findByProductId(productId);
        
        assertThat(psws).hasSize(2);
        assertThat(psws).extracting(ProductSellWindow::getMinTotalQty)
                .containsExactlyInAnyOrder(100, 200);
    }

    @Test
    void testFindBySellWindowId() {
        Product product2 = createProduct("Product 2");
        ProductSellWindow psw1 = createProductSellWindow(testProduct, testSellWindow, 100);
        ProductSellWindow psw2 = createProductSellWindow(product2, testSellWindow, 150);
        
        entityManager.persistAndFlush(psw1);
        entityManager.persistAndFlush(psw2);
        
        List<ProductSellWindow> psws = productSellWindowRepository.findBySellWindowId(sellWindowId);
        
        assertThat(psws).hasSize(2);
    }

    @Test
    void testFindByProductIdAndSellWindowId() {
        ProductSellWindow psw = createProductSellWindow(testProduct, testSellWindow, 100);
        entityManager.persistAndFlush(psw);
        
        Optional<ProductSellWindow> found = productSellWindowRepository
                .findByProductIdAndSellWindowId(productId, sellWindowId);
        
        assertThat(found).isPresent();
        assertThat(found.get().getMinTotalQty()).isEqualTo(100);
    }

    @Test
    void testFindByEnabled() {
        ProductSellWindow enabledPsw = createProductSellWindow(testProduct, testSellWindow, 100);
        enabledPsw.setClosed(true);
        
        ProductSellWindow disabledPsw = createProductSellWindow(testProduct, createSellWindow("Disabled"), 200);
        disabledPsw.setClosed(false);
        
        entityManager.persistAndFlush(enabledPsw);
        entityManager.persistAndFlush(disabledPsw);
        
        List<ProductSellWindow> enabledPsws = productSellWindowRepository.findByIsClosed(true);
        
        assertThat(enabledPsws).hasSize(1);
        assertThat(enabledPsws.get(0).isClosed()).isTrue();
    }

    @Test
    void testDeleteProductSellWindow() {
        ProductSellWindow psw = createProductSellWindow(testProduct, testSellWindow, 100);
        ProductSellWindow saved = entityManager.persistAndFlush(psw);
        UUID pswId = saved.getId();
        
        productSellWindowRepository.delete(saved);
        entityManager.flush();
        
        assertThat(productSellWindowRepository.findById(pswId)).isEmpty();
    }

    private ProductSellWindow createProductSellWindow(Product product, SellWindow sellWindow, int thresholdQty) {
        ProductSellWindow psw = new ProductSellWindow();
        psw.setId(UUID.randomUUID());
        psw.setProduct(product);
        psw.setSellWindow(sellWindow);
        psw.setMinTotalQty(thresholdQty);
        psw.setMaxTotalQty(500);
        psw.setLeadDays(7);
        psw.setShipDays(14);
        psw.setClosed(true);
        return psw;
    }

    private Product createProduct(String name) {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName(name);
        product.setStatus(ProductStatus.ACTIVE);
        return entityManager.persistAndFlush(product);
    }

    private SellWindow createSellWindow(String name) {
        SellWindow sellWindow = new SellWindow();
        sellWindow.setId(UUID.randomUUID());
        sellWindow.setName(name);
        sellWindow.setStartAt(OffsetDateTime.now());
        sellWindow.setEndAt(OffsetDateTime.now().plusMonths(3));
        sellWindow.setTimezone("Asia/Taipei");
        return entityManager.persistAndFlush(sellWindow);
    }
}
