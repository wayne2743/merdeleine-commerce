package com.merdeleine.production.repository;

import com.merdeleine.production.entity.WorkOrder;
import com.merdeleine.production.entity.WorkStep;
import com.merdeleine.production.enums.WorkOrderStatus;
import com.merdeleine.production.enums.WorkStepStatus;
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
class WorkStepRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("production_db")
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
    private WorkStepRepository workStepRepository;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    private WorkOrder testWorkOrder;

    @BeforeEach
    void setUp() {
        testWorkOrder = new WorkOrder();
        testWorkOrder.setId(UUID.randomUUID());
        testWorkOrder.setBatchId(UUID.randomUUID());
        testWorkOrder.setStatus(WorkOrderStatus.READY);
        entityManager.persistAndFlush(testWorkOrder);
    }

    @Test
    void testSaveWorkStep() {
        WorkStep step = new WorkStep();
        step.setId(UUID.randomUUID());
        step.setWorkOrder(testWorkOrder);
        step.setStepName("Prepare materials");
        step.setStatus(WorkStepStatus.TODO);
        step.setNotes("Gather all required materials");
        
        WorkStep saved = workStepRepository.save(step);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStepName()).isEqualTo("Prepare materials");
        assertThat(saved.getStatus()).isEqualTo(WorkStepStatus.TODO);
        assertThat(saved.getNotes()).isEqualTo("Gather all required materials");
    }

    @Test
    void testFindByWorkOrderId() {
        WorkStep step1 = createStep("Step 1", WorkStepStatus.TODO);
        WorkStep step2 = createStep("Step 2", WorkStepStatus.DOING);
        WorkStep step3 = createStep("Step 3", WorkStepStatus.DONE);
        
        entityManager.persistAndFlush(step1);
        entityManager.persistAndFlush(step2);
        entityManager.persistAndFlush(step3);
        
        List<WorkStep> steps = workStepRepository.findByWorkOrderId(testWorkOrder.getId());
        
        assertThat(steps).hasSize(3);
        assertThat(steps).extracting(WorkStep::getStepName)
                .containsExactlyInAnyOrder("Step 1", "Step 2", "Step 3");
    }

    @Test
    void testUpdateWorkStepStatus() {
        WorkStep step = createStep("Test step", WorkStepStatus.TODO);
        WorkStep saved = entityManager.persistAndFlush(step);
        
        saved.setStatus(WorkStepStatus.DOING);
        saved.setNotes("In progress");
        
        WorkStep updated = workStepRepository.save(saved);
        
        assertThat(updated.getStatus()).isEqualTo(WorkStepStatus.DOING);
        assertThat(updated.getNotes()).isEqualTo("In progress");
    }

    @Test
    void testCompleteWorkStep() {
        WorkStep step = createStep("Test step", WorkStepStatus.DOING);
        WorkStep saved = entityManager.persistAndFlush(step);
        
        saved.setStatus(WorkStepStatus.DONE);
        saved.setNotes("Completed successfully");
        
        WorkStep completed = workStepRepository.save(saved);
        
        assertThat(completed.getStatus()).isEqualTo(WorkStepStatus.DONE);
        assertThat(completed.getNotes()).isEqualTo("Completed successfully");
    }

    @Test
    void testDeleteWorkStep() {
        WorkStep step = createStep("Test step", WorkStepStatus.TODO);
        WorkStep saved = entityManager.persistAndFlush(step);
        UUID stepId = saved.getId();
        
        workStepRepository.delete(saved);
        entityManager.flush();
        
        assertThat(workStepRepository.findById(stepId)).isEmpty();
    }

    private WorkStep createStep(String stepName, WorkStepStatus status) {
        WorkStep step = new WorkStep();
        step.setId(UUID.randomUUID());
        step.setWorkOrder(testWorkOrder);
        step.setStepName(stepName);
        step.setStatus(status);
        return step;
    }
}
