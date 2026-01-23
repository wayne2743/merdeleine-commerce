package com.merdeleine.catalog.repository;

import com.merdeleine.catalog.entity.Product;
import com.merdeleine.catalog.entity.ProductVariant;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ProductVariantRepositoryTest {

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
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private ProductRepository productRepository;

    private Product testProduct;
    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        
        testProduct = new Product();
        testProduct.setId(productId);
        testProduct.setName("Test Product");
        testProduct.setStatus(ProductStatus.ACTIVE);
        entityManager.persistAndFlush(testProduct);
    }

    @Test
    void testSaveProductVariant() {
        ProductVariant variant = new ProductVariant();
        variant.setId(UUID.randomUUID());
        variant.setProduct(testProduct);
        variant.setSku("SKU-001");
        variant.setVariantName("Variant 1");
        variant.setPriceCents(10000);
        variant.setCurrency("TWD");
        variant.setActive(true);
        
        ProductVariant saved = productVariantRepository.saveAndFlush(variant);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSku()).isEqualTo("SKU-001");
        assertThat(saved.getVariantName()).isEqualTo("Variant 1");
        assertThat(saved.getPriceCents()).isEqualTo(10000);
    }

    @Test
    void testFindByProductId() {
        ProductVariant variant1 = createVariant(testProduct, "SKU-001", "Variant 1");
        ProductVariant variant2 = createVariant(testProduct, "SKU-002", "Variant 2");
        
        entityManager.persistAndFlush(variant1);
        entityManager.persistAndFlush(variant2);
        
        List<ProductVariant> variants = productVariantRepository.findByProductId(productId);
        
        assertThat(variants).hasSize(2);
        assertThat(variants).extracting(ProductVariant::getSku)
                .containsExactlyInAnyOrder("SKU-001", "SKU-002");
    }

    @Test
    void testFindBySku() {
        ProductVariant variant = createVariant(testProduct, "SKU-001", "Variant 1");
        entityManager.persistAndFlush(variant);
        
        Optional<ProductVariant> found = productVariantRepository.findBySku("SKU-001");
        
        assertThat(found).isPresent();
        assertThat(found.get().getSku()).isEqualTo("SKU-001");
    }

    @Test
    void testFindByIsActive() {
        ProductVariant activeVariant = createVariant(testProduct, "SKU-001", "Variant 1");
        activeVariant.setActive(true);
        
        ProductVariant inactiveVariant = createVariant(testProduct, "SKU-002", "Variant 2");
        inactiveVariant.setActive(false);
        
        entityManager.persistAndFlush(activeVariant);
        entityManager.persistAndFlush(inactiveVariant);
        
        List<ProductVariant> activeVariants = productVariantRepository.findByIsActive(true);
        
        assertThat(activeVariants).hasSize(1);
        assertThat(activeVariants.get(0).getSku()).isEqualTo("SKU-001");
    }

    @Test
    void testDeleteProductVariant() {
        ProductVariant variant = createVariant(testProduct, "SKU-001", "Variant 1");
        ProductVariant saved = entityManager.persistAndFlush(variant);
        UUID variantId = saved.getId();
        
        productVariantRepository.delete(saved);
        entityManager.flush();
        
        assertThat(productVariantRepository.findById(variantId)).isEmpty();
    }

    private ProductVariant createVariant(Product product, String sku, String variantName) {
        ProductVariant variant = new ProductVariant();
        variant.setId(UUID.randomUUID());
        variant.setProduct(product);
        variant.setSku(sku);
        variant.setVariantName(variantName);
        variant.setPriceCents(10000);
        variant.setCurrency("TWD");
        variant.setActive(true);
        return variant;
    }
}
