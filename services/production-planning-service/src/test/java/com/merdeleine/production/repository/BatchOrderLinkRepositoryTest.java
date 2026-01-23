package com.merdeleine.production.repository;

import com.merdeleine.production.entity.Batch;
import com.merdeleine.production.entity.BatchOrderLink;
import com.merdeleine.production.enums.BatchStatus;
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
class BatchOrderLinkRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("production_planning_db")
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
    private BatchOrderLinkRepository batchOrderLinkRepository;

    @Autowired
    private BatchRepository batchRepository;

    private Batch testBatch;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        
        testBatch = new Batch();
        testBatch.setId(UUID.randomUUID());
        testBatch.setSellWindowId(UUID.randomUUID());
        testBatch.setProductId(UUID.randomUUID());
        testBatch.setTargetQty(100);
        testBatch.setStatus(BatchStatus.CREATED);
        entityManager.persistAndFlush(testBatch);
    }

    @Test
    void testSaveBatchOrderLink() {
        BatchOrderLink link = new BatchOrderLink();
        link.setId(UUID.randomUUID());
        link.setBatch(testBatch);
        link.setOrderId(orderId);
        link.setQuantity(50);
        
        BatchOrderLink saved = batchOrderLinkRepository.save(link);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOrderId()).isEqualTo(orderId);
        assertThat(saved.getQuantity()).isEqualTo(50);
    }

    @Test
    void testFindByBatchId() {
        BatchOrderLink link1 = createLink(testBatch, UUID.randomUUID(), 30);
        BatchOrderLink link2 = createLink(testBatch, UUID.randomUUID(), 70);
        
        entityManager.persistAndFlush(link1);
        entityManager.persistAndFlush(link2);
        
        List<BatchOrderLink> links = batchOrderLinkRepository.findByBatchId(testBatch.getId());
        
        assertThat(links).hasSize(2);
        assertThat(links).extracting(BatchOrderLink::getQuantity).containsExactlyInAnyOrder(30, 70);
    }

    @Test
    void testFindByOrderId() {
        BatchOrderLink link = createLink(testBatch, orderId, 50);
        entityManager.persistAndFlush(link);
        
        List<BatchOrderLink> links = batchOrderLinkRepository.findByOrderId(orderId);
        
        assertThat(links).hasSize(1);
        assertThat(links.get(0).getOrderId()).isEqualTo(orderId);
    }

    @Test
    void testMultipleBatchesForSameOrder() {
        Batch batch2 = createBatch();
        entityManager.persistAndFlush(batch2);
        
        BatchOrderLink link1 = createLink(testBatch, orderId, 30);
        BatchOrderLink link2 = createLink(batch2, orderId, 20);
        
        entityManager.persistAndFlush(link1);
        entityManager.persistAndFlush(link2);
        
        List<BatchOrderLink> links = batchOrderLinkRepository.findByOrderId(orderId);
        
        assertThat(links).hasSize(2);
    }

    @Test
    void testDeleteBatchOrderLink() {
        BatchOrderLink link = createLink(testBatch, orderId, 50);
        BatchOrderLink saved = entityManager.persistAndFlush(link);
        UUID linkId = saved.getId();
        
        batchOrderLinkRepository.delete(saved);
        entityManager.flush();
        
        assertThat(batchOrderLinkRepository.findById(linkId)).isEmpty();
    }

    private BatchOrderLink createLink(Batch batch, UUID orderId, int quantity) {
        BatchOrderLink link = new BatchOrderLink();
        link.setId(UUID.randomUUID());
        link.setBatch(batch);
        link.setOrderId(orderId);
        link.setQuantity(quantity);
        return link;
    }

    private Batch createBatch() {
        Batch batch = new Batch();
        batch.setId(UUID.randomUUID());
        batch.setSellWindowId(UUID.randomUUID());
        batch.setProductId(UUID.randomUUID());
        batch.setTargetQty(100);
        batch.setStatus(BatchStatus.CREATED);
        return batch;
    }
}
