package com.merdeleine.production.repository;

import com.merdeleine.production.entity.Batch;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class BatchScheduleRepositoryTest {

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
    private BatchScheduleRepository batchScheduleRepository;

    @Autowired
    private BatchRepository batchRepository;

    private Batch testBatch;

    @BeforeEach
    void setUp() {
        testBatch = new Batch();
        testBatch.setId(UUID.randomUUID());
        testBatch.setSellWindowId(UUID.randomUUID());
        testBatch.setProductId(UUID.randomUUID());
        testBatch.setTargetQty(100);
        testBatch.setStatus(BatchStatus.CREATED);
        entityManager.persistAndFlush(testBatch);
    }

    @Test
    void testSaveBatchSchedule() {
        BatchSchedule schedule = new BatchSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setBatch(testBatch);
        schedule.setPlannedProductionDate(OffsetDateTime.now().plusDays(7));
        schedule.setPlannedShipDate(OffsetDateTime.now().plusDays(14));
        schedule.setNotes("Production schedule");
        
        BatchSchedule saved = batchScheduleRepository.saveAndFlush(schedule);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPlannedProductionDate()).isNotNull();
        assertThat(saved.getPlannedShipDate()).isNotNull();
        assertThat(saved.getNotes()).isEqualTo("Production schedule");
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void testFindByBatchId() {
        BatchSchedule schedule = createSchedule();
        entityManager.persistAndFlush(schedule);
        
        Optional<BatchSchedule> found = batchScheduleRepository.findByBatchId(testBatch.getId());
        
        assertThat(found).isPresent();
        assertThat(found.get().getBatch().getId()).isEqualTo(testBatch.getId());
    }

    @Test
    void testUpdateBatchSchedule() {
        BatchSchedule schedule = createSchedule();
        BatchSchedule saved = entityManager.persistAndFlush(schedule);
        
        OffsetDateTime newProductionDate = OffsetDateTime.now().plusDays(10);
        saved.setPlannedProductionDate(newProductionDate);
        saved.setNotes("Updated schedule");
        
        BatchSchedule updated = batchScheduleRepository.save(saved);
        
        assertThat(updated.getPlannedProductionDate()).isEqualTo(newProductionDate);
        assertThat(updated.getNotes()).isEqualTo("Updated schedule");
    }

    @Test
    void testScheduleWithoutDates() {
        BatchSchedule schedule = new BatchSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setBatch(testBatch);
        schedule.setNotes("Schedule without dates");
        
        BatchSchedule saved = batchScheduleRepository.save(schedule);
        
        assertThat(saved.getPlannedProductionDate()).isNull();
        assertThat(saved.getPlannedShipDate()).isNull();
        assertThat(saved.getNotes()).isEqualTo("Schedule without dates");
    }

    @Test
    void testDeleteBatchSchedule() {
        BatchSchedule schedule = createSchedule();
        BatchSchedule saved = entityManager.persistAndFlush(schedule);
        UUID scheduleId = saved.getId();
        
        batchScheduleRepository.delete(saved);
        entityManager.flush();
        
        assertThat(batchScheduleRepository.findById(scheduleId)).isEmpty();
    }

    private BatchSchedule createSchedule() {
        BatchSchedule schedule = new BatchSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setBatch(testBatch);
        schedule.setPlannedProductionDate(OffsetDateTime.now().plusDays(7));
        schedule.setPlannedShipDate(OffsetDateTime.now().plusDays(14));
        schedule.setNotes("Test schedule");
        return schedule;
    }
}
