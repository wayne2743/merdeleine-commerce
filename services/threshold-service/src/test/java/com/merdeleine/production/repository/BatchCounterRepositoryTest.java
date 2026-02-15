package com.merdeleine.production.repository;

import com.merdeleine.production.entity.BatchCounter;
import com.merdeleine.production.entity.CounterEventLog;
import com.merdeleine.production.enums.CounterStatus;
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
class BatchCounterRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("threshold_db")
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
    private BatchCounterRepository batchCounterRepository;

    @Autowired
    private CounterEventLogRepository counterEventLogRepository;

    private UUID sellWindowId;
    private UUID productId;
    private BatchCounter testCounter;

    @BeforeEach
    void setUp() {
        sellWindowId = UUID.randomUUID();
        productId = UUID.randomUUID();
        
        testCounter = new BatchCounter();
        testCounter.setId(UUID.randomUUID());
        testCounter.setSellWindowId(sellWindowId);
        testCounter.setProductId(productId);
        testCounter.setPaidQty(0);
        testCounter.setThresholdQty(100);
        testCounter.setStatus(CounterStatus.OPEN);
    }

    @Test
    void testSaveBatchCounter() {
        BatchCounter saved = batchCounterRepository.saveAndFlush(testCounter);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSellWindowId()).isEqualTo(sellWindowId);
        assertThat(saved.getProductId()).isEqualTo(productId);
        assertThat(saved.getPaidQty()).isEqualTo(0);
        assertThat(saved.getThresholdQty()).isEqualTo(100);
        assertThat(saved.getStatus()).isEqualTo(CounterStatus.OPEN);
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void testFindBySellWindowIdAndProductId() {
        entityManager.persistAndFlush(testCounter);
        
        Optional<BatchCounter> found = batchCounterRepository.findBySellWindowIdAndProductId(sellWindowId, productId);
        
        assertThat(found).isPresent();
        assertThat(found.get().getSellWindowId()).isEqualTo(sellWindowId);
        assertThat(found.get().getProductId()).isEqualTo(productId);
    }

    @Test
    void testFindBySellWindowId() {
        entityManager.persistAndFlush(testCounter);
        
        BatchCounter counter2 = createCounter(sellWindowId, UUID.randomUUID());
        entityManager.persistAndFlush(counter2);
        
        List<BatchCounter> counters = batchCounterRepository.findBySellWindowId(sellWindowId);
        
        assertThat(counters).hasSize(2);
        assertThat(counters).extracting(BatchCounter::getSellWindowId).containsOnly(sellWindowId);
    }

    @Test
    void testFindByProductId() {
        entityManager.persistAndFlush(testCounter);
        
        List<BatchCounter> counters = batchCounterRepository.findByProductId(productId);
        
        assertThat(counters).hasSize(1);
        assertThat(counters.get(0).getProductId()).isEqualTo(productId);
    }

    @Test
    void testFindByStatus() {
        entityManager.persistAndFlush(testCounter);
        
        BatchCounter reachedCounter = createCounter(UUID.randomUUID(), UUID.randomUUID());
        reachedCounter.setStatus(CounterStatus.REACHED);
        reachedCounter.setReachedAt(OffsetDateTime.now());
        entityManager.persistAndFlush(reachedCounter);
        
        List<BatchCounter> openCounters = batchCounterRepository.findByStatus(CounterStatus.OPEN);
        
        assertThat(openCounters).hasSize(1);
        assertThat(openCounters.get(0).getStatus()).isEqualTo(CounterStatus.OPEN);
    }

    @Test
    void testCounterReachedThreshold() {
        BatchCounter saved = entityManager.persistAndFlush(testCounter);
        saved.setPaidQty(100);
        saved.setStatus(CounterStatus.REACHED);
        saved.setReachedAt(OffsetDateTime.now());
        saved.setReachedEventId(UUID.randomUUID());
        
        BatchCounter updated = batchCounterRepository.save(saved);
        
        assertThat(updated.getPaidQty()).isEqualTo(100);
        assertThat(updated.getStatus()).isEqualTo(CounterStatus.REACHED);
        assertThat(updated.getReachedAt()).isNotNull();
        assertThat(updated.getReachedEventId()).isNotNull();
    }

    @Test
    void testCounterWithEventLogs() {
        BatchCounter saved = entityManager.persistAndFlush(testCounter);
        
        CounterEventLog log1 = createEventLog(saved, "order.paid.v1", 10);
        CounterEventLog log2 = createEventLog(saved, "order.paid.v1", 20);
        
        entityManager.persistAndFlush(log1);
        entityManager.persistAndFlush(log2);
        entityManager.clear();
        
        List<CounterEventLog> logs = counterEventLogRepository.findByCounterId(saved.getId());
        
        assertThat(logs).hasSize(2);
        assertThat(logs).extracting(CounterEventLog::getDeltaQty).containsExactlyInAnyOrder(10, 20);
    }

    @Test
    void testUpdateCounterQuantity() {
        BatchCounter saved = entityManager.persistAndFlush(testCounter);
        saved.setPaidQty(50);
        
        BatchCounter updated = batchCounterRepository.save(saved);
        
        assertThat(updated.getPaidQty()).isEqualTo(50);
    }

    private BatchCounter createCounter(UUID sellWindowId, UUID productId) {
        BatchCounter counter = new BatchCounter();
        counter.setId(UUID.randomUUID());
        counter.setSellWindowId(sellWindowId);
        counter.setProductId(productId);
        counter.setPaidQty(0);
        counter.setThresholdQty(100);
        counter.setStatus(CounterStatus.OPEN);
        return counter;
    }

    private CounterEventLog createEventLog(BatchCounter counter, String eventType, int deltaQty) {
        CounterEventLog log = new CounterEventLog();
        log.setId(UUID.randomUUID());
        log.setCounter(counter);
        log.setSourceEventType(eventType);
        log.setSourceEventId(UUID.randomUUID());
        log.setDeltaQty(deltaQty);
        return log;
    }
}
