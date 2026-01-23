package com.merdeleine.catalog.repository;

import com.merdeleine.catalog.entity.Product;
import com.merdeleine.catalog.domain.ProductStatus;
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
class ProductRepositoryTest {

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
    private ProductRepository productRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(UUID.randomUUID());
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setStatus(ProductStatus.DRAFT);
    }

    @Test
    void testSaveProduct() {
        Product saved = productRepository.saveAndFlush(testProduct);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Test Product");
        assertThat(saved.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void testFindByStatus() {
        entityManager.persistAndFlush(testProduct);
        
        Product activeProduct = createProduct("Active Product", ProductStatus.ACTIVE);
        entityManager.persistAndFlush(activeProduct);
        
        List<Product> draftProducts = productRepository.findByStatus(ProductStatus.DRAFT);
        
        assertThat(draftProducts).hasSize(1);
        assertThat(draftProducts.get(0).getStatus()).isEqualTo(ProductStatus.DRAFT);
    }

    @Test
    void testUpdateProductStatus() {
        Product saved = entityManager.persistAndFlush(testProduct);
        saved.setStatus(ProductStatus.ACTIVE);
        Product updated = productRepository.saveAndFlush(saved);
        
        assertThat(updated.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(updated.getUpdatedAt()).isAfter(saved.getCreatedAt());
    }

    @Test
    void testDeleteProduct() {
        Product saved = entityManager.persistAndFlush(testProduct);
        UUID productId = saved.getId();
        
        productRepository.delete(saved);
        entityManager.flush();
        
        assertThat(productRepository.findById(productId)).isEmpty();
    }

    private Product createProduct(String name, ProductStatus status) {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName(name);
        product.setStatus(status);
        return product;
    }
}
