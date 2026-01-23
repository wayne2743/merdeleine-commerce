package com.merdeleine.production.repository;

import com.merdeleine.production.entity.Batch;
import com.merdeleine.production.entity.BatchOrderLink;
import com.merdeleine.production.entity.BatchSchedule;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class BatchRepositoryTest {

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
    private BatchRepository batchRepository;

    @Autowired
    private BatchOrderLinkRepository batchOrderLinkRepository;

    @Autowired
    private BatchScheduleRepository batchScheduleRepository;

    private UUID sellWindowId;
    private UUID productId;
    private Batch testBatch;

    @BeforeEach
    void setUp() {
        sellWindowId = UUID.randomUUID();
        productId = UUID.randomUUID();
        
        testBatch = new Batch();
        testBatch.setId(UUID.randomUUID());
        testBatch.setSellWindowId(sellWindowId);
        testBatch.setProductId(productId);
        testBatch.setTargetQty(100);
        testBatch.setStatus(BatchStatus.CREATED);
    }

    @Test
    void testSaveBatch() {
        Batch saved = batchRepository.saveAndFlush(testBatch);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSellWindowId()).isEqualTo(sellWindowId);
        assertThat(saved.getProductId()).isEqualTo(productId);
        assertThat(saved.getTargetQty()).isEqualTo(100);
        assertThat(saved.getStatus()).isEqualTo(BatchStatus.CREATED);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void testFindBySellWindowId() {
        entityManager.persistAndFlush(testBatch);
        
        Batch batch2 = createBatch(sellWindowId, UUID.randomUUID());
        entityManager.persistAndFlush(batch2);
        
        List<Batch> batches = batchRepository.findBySellWindowId(sellWindowId);
        
        assertThat(batches).hasSize(2);
        assertThat(batches).extracting(Batch::getSellWindowId).containsOnly(sellWindowId);
    }

    @Test
    void testFindByProductId() {
        entityManager.persistAndFlush(testBatch);
        
        List<Batch> batches = batchRepository.findByProductId(productId);
        
        assertThat(batches).hasSize(1);
        assertThat(batches.get(0).getProductId()).isEqualTo(productId);
    }

    @Test
    void testFindByStatus() {
        entityManager.persistAndFlush(testBatch);
        
        Batch confirmedBatch = createBatch(UUID.randomUUID(), UUID.randomUUID());
        confirmedBatch.setStatus(BatchStatus.CONFIRMED);
        confirmedBatch.setConfirmedAt(OffsetDateTime.now());
        entityManager.persistAndFlush(confirmedBatch);
        
        List<Batch> createdBatches = batchRepository.findByStatus(BatchStatus.CREATED);
        
        assertThat(createdBatches).hasSize(1);
        assertThat(createdBatches.get(0).getStatus()).isEqualTo(BatchStatus.CREATED);
    }

    @Test
    void testFindBySellWindowIdAndProductId() {
        entityManager.persistAndFlush(testBatch);
        
        List<Batch> batches = batchRepository.findBySellWindowIdAndProductId(sellWindowId, productId);
        
        assertThat(batches).hasSize(1);
        assertThat(batches.get(0).getSellWindowId()).isEqualTo(sellWindowId);
        assertThat(batches.get(0).getProductId()).isEqualTo(productId);
    }

    @Test
    void testBatchWithOrderLinks() {
        Batch saved = entityManager.persistAndFlush(testBatch);
        
        BatchOrderLink link1 = createOrderLink(saved, UUID.randomUUID(), 50);
        BatchOrderLink link2 = createOrderLink(saved, UUID.randomUUID(), 50);
        
        entityManager.persistAndFlush(link1);
        entityManager.persistAndFlush(link2);
        entityManager.clear();
        
        List<BatchOrderLink> links = batchOrderLinkRepository.findByBatchId(saved.getId());
        
        assertThat(links).hasSize(2);
        assertThat(links).extracting(BatchOrderLink::getQuantity).containsExactlyInAnyOrder(50, 50);
    }

    @Test
    void testBatchWithSchedule() {
        Batch saved = entityManager.persistAndFlush(testBatch);
        
        BatchSchedule schedule = new BatchSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setBatch(saved);
        schedule.setPlannedProductionDate(OffsetDateTime.now().plusDays(7));
        schedule.setPlannedShipDate(OffsetDateTime.now().plusDays(14));
        schedule.setNotes("Test schedule");
        
        entityManager.persistAndFlush(schedule);
        entityManager.clear();
        
        BatchSchedule found = batchScheduleRepository.findByBatchId(saved.getId()).orElseThrow();
        
        assertThat(found.getPlannedProductionDate()).isNotNull();
        assertThat(found.getPlannedShipDate()).isNotNull();
        assertThat(found.getNotes()).isEqualTo("Test schedule");
    }

    @Test
    void testUpdateBatchStatus() {
        Batch saved = entityManager.persistAndFlush(testBatch);
        saved.setStatus(BatchStatus.CONFIRMED);
        saved.setConfirmedAt(OffsetDateTime.now());
        
        Batch updated = batchRepository.save(saved);
        
        assertThat(updated.getStatus()).isEqualTo(BatchStatus.CONFIRMED);
        assertThat(updated.getConfirmedAt()).isNotNull();
    }

    private Batch createBatch(UUID sellWindowId, UUID productId) {
        Batch batch = new Batch();
        batch.setId(UUID.randomUUID());
        batch.setSellWindowId(sellWindowId);
        batch.setProductId(productId);
        batch.setTargetQty(100);
        batch.setStatus(BatchStatus.CREATED);
        return batch;
    }

    private BatchOrderLink createOrderLink(Batch batch, UUID orderId, int quantity) {
        BatchOrderLink link = new BatchOrderLink();
        link.setId(UUID.randomUUID());
        link.setBatch(batch);
        link.setOrderId(orderId);
        link.setQuantity(quantity);
        return link;
    }
}
