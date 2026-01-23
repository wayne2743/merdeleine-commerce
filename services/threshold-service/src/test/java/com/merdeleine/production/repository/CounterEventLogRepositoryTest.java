package com.merdeleine.production.repository;

import com.merdeleine.production.entity.BatchCounter;
import com.merdeleine.production.entity.CounterEventLog;
import com.merdeleine.production.enums.BatchCounterStatus;
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
class CounterEventLogRepositoryTest {

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
    private CounterEventLogRepository counterEventLogRepository;

    @Autowired
    private BatchCounterRepository batchCounterRepository;

    private BatchCounter testCounter;
    private UUID sourceEventId;

    @BeforeEach
    void setUp() {
        sourceEventId = UUID.randomUUID();
        
        testCounter = new BatchCounter();
        testCounter.setId(UUID.randomUUID());
        testCounter.setSellWindowId(UUID.randomUUID());
        testCounter.setProductId(UUID.randomUUID());
        testCounter.setPaidQty(0);
        testCounter.setThresholdQty(100);
        testCounter.setStatus(BatchCounterStatus.OPEN);
        entityManager.persistAndFlush(testCounter);
    }

    @Test
    void testSaveCounterEventLog() {
        CounterEventLog log = new CounterEventLog();
        log.setId(UUID.randomUUID());
        log.setCounter(testCounter);
        log.setSourceEventType("order.paid.v1");
        log.setSourceEventId(sourceEventId);
        log.setDeltaQty(10);
        
        CounterEventLog saved = counterEventLogRepository.saveAndFlush(log);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSourceEventType()).isEqualTo("order.paid.v1");
        assertThat(saved.getSourceEventId()).isEqualTo(sourceEventId);
        assertThat(saved.getDeltaQty()).isEqualTo(10);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void testFindByCounterId() {
        CounterEventLog log1 = createLog("order.paid.v1", UUID.randomUUID(), 10);
        CounterEventLog log2 = createLog("order.paid.v1", UUID.randomUUID(), 20);
        CounterEventLog log3 = createLog("order.refunded.v1", UUID.randomUUID(), -5);
        
        entityManager.persistAndFlush(log1);
        entityManager.persistAndFlush(log2);
        entityManager.persistAndFlush(log3);
        
        List<CounterEventLog> logs = counterEventLogRepository.findByCounterId(testCounter.getId());
        
        assertThat(logs).hasSize(3);
        assertThat(logs).extracting(CounterEventLog::getDeltaQty).containsExactlyInAnyOrder(10, 20, -5);
    }

    @Test
    void testFindBySourceEventId() {
        CounterEventLog log = createLog("order.paid.v1", sourceEventId, 10);
        entityManager.persistAndFlush(log);
        
        List<CounterEventLog> logs = counterEventLogRepository.findBySourceEventId(sourceEventId);
        
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getSourceEventId()).isEqualTo(sourceEventId);
    }

    @Test
    void testMultipleCountersWithSameEvent() {
        BatchCounter counter2 = createCounter();
        entityManager.persistAndFlush(counter2);
        
        CounterEventLog log1 = createLog("order.paid.v1", sourceEventId, 10);
        CounterEventLog log2 = createLog("order.paid.v1", sourceEventId, 15);
        log2.setCounter(counter2);
        
        entityManager.persistAndFlush(log1);
        entityManager.persistAndFlush(log2);
        
        List<CounterEventLog> logs = counterEventLogRepository.findBySourceEventId(sourceEventId);
        
        assertThat(logs).hasSize(2);
    }

    @Test
    void testNegativeDeltaQty() {
        CounterEventLog log = createLog("order.refunded.v1", UUID.randomUUID(), -10);
        CounterEventLog saved = counterEventLogRepository.save(log);
        
        assertThat(saved.getDeltaQty()).isEqualTo(-10);
    }

    @Test
    void testDeleteCounterEventLog() {
        CounterEventLog log = createLog("order.paid.v1", sourceEventId, 10);
        CounterEventLog saved = entityManager.persistAndFlush(log);
        UUID logId = saved.getId();
        
        counterEventLogRepository.delete(saved);
        entityManager.flush();
        
        assertThat(counterEventLogRepository.findById(logId)).isEmpty();
    }

    private CounterEventLog createLog(String eventType, UUID eventId, int deltaQty) {
        CounterEventLog log = new CounterEventLog();
        log.setId(UUID.randomUUID());
        log.setCounter(testCounter);
        log.setSourceEventType(eventType);
        log.setSourceEventId(eventId);
        log.setDeltaQty(deltaQty);
        return log;
    }

    private BatchCounter createCounter() {
        BatchCounter counter = new BatchCounter();
        counter.setId(UUID.randomUUID());
        counter.setSellWindowId(UUID.randomUUID());
        counter.setProductId(UUID.randomUUID());
        counter.setPaidQty(0);
        counter.setThresholdQty(100);
        counter.setStatus(BatchCounterStatus.OPEN);
        return counter;
    }
}
