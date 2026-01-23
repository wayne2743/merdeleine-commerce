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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class WorkOrderRepositoryTest {

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
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private WorkStepRepository workStepRepository;

    private UUID batchId;
    private WorkOrder testWorkOrder;

    @BeforeEach
    void setUp() {
        batchId = UUID.randomUUID();
        
        testWorkOrder = new WorkOrder();
        testWorkOrder.setId(UUID.randomUUID());
        testWorkOrder.setBatchId(batchId);
        testWorkOrder.setStatus(WorkOrderStatus.READY);
    }

    @Test
    void testSaveWorkOrder() {
        WorkOrder saved = workOrderRepository.save(testWorkOrder);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getBatchId()).isEqualTo(batchId);
        assertThat(saved.getStatus()).isEqualTo(WorkOrderStatus.READY);
    }

    @Test
    void testFindByBatchId() {
        entityManager.persistAndFlush(testWorkOrder);
        
        WorkOrder workOrder2 = createWorkOrder(batchId, WorkOrderStatus.IN_PROGRESS);
        entityManager.persistAndFlush(workOrder2);
        
        List<WorkOrder> workOrders = workOrderRepository.findByBatchId(batchId);
        
        assertThat(workOrders).hasSize(2);
        assertThat(workOrders).extracting(WorkOrder::getBatchId).containsOnly(batchId);
    }

    @Test
    void testFindByStatus() {
        entityManager.persistAndFlush(testWorkOrder);
        
        WorkOrder inProgressOrder = createWorkOrder(UUID.randomUUID(), WorkOrderStatus.IN_PROGRESS);
        inProgressOrder.setStartAt(OffsetDateTime.now());
        entityManager.persistAndFlush(inProgressOrder);
        
        List<WorkOrder> readyOrders = workOrderRepository.findByStatus(WorkOrderStatus.READY);
        
        assertThat(readyOrders).hasSize(1);
        assertThat(readyOrders.get(0).getStatus()).isEqualTo(WorkOrderStatus.READY);
    }

    @Test
    void testWorkOrderWithSteps() {
        WorkOrder saved = entityManager.persistAndFlush(testWorkOrder);
        
        WorkStep step1 = createStep(saved, "Prepare materials", WorkStepStatus.TODO);
        WorkStep step2 = createStep(saved, "Assemble", WorkStepStatus.TODO);
        
        entityManager.persistAndFlush(step1);
        entityManager.persistAndFlush(step2);
        entityManager.clear();
        
        List<WorkStep> steps = workStepRepository.findByWorkOrderId(saved.getId());
        
        assertThat(steps).hasSize(2);
        assertThat(steps).extracting(WorkStep::getStepName)
                .containsExactlyInAnyOrder("Prepare materials", "Assemble");
    }

    @Test
    void testUpdateWorkOrderStatus() {
        WorkOrder saved = entityManager.persistAndFlush(testWorkOrder);
        saved.setStatus(WorkOrderStatus.IN_PROGRESS);
        saved.setStartAt(OffsetDateTime.now());
        saved.setOperator("Operator1");
        
        WorkOrder updated = workOrderRepository.save(saved);
        
        assertThat(updated.getStatus()).isEqualTo(WorkOrderStatus.IN_PROGRESS);
        assertThat(updated.getStartAt()).isNotNull();
        assertThat(updated.getOperator()).isEqualTo("Operator1");
    }

    @Test
    void testCompleteWorkOrder() {
        WorkOrder saved = entityManager.persistAndFlush(testWorkOrder);
        saved.setStatus(WorkOrderStatus.DONE);
        saved.setStartAt(OffsetDateTime.now().minusHours(2));
        saved.setEndAt(OffsetDateTime.now());
        
        WorkOrder completed = workOrderRepository.save(saved);
        
        assertThat(completed.getStatus()).isEqualTo(WorkOrderStatus.DONE);
        assertThat(completed.getStartAt()).isNotNull();
        assertThat(completed.getEndAt()).isNotNull();
    }

    @Test
    void testDeleteWorkOrder() {
        WorkOrder saved = entityManager.persistAndFlush(testWorkOrder);
        UUID workOrderId = saved.getId();
        
        workOrderRepository.delete(saved);
        entityManager.flush();
        
        assertThat(workOrderRepository.findById(workOrderId)).isEmpty();
    }

    private WorkOrder createWorkOrder(UUID batchId, WorkOrderStatus status) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(UUID.randomUUID());
        workOrder.setBatchId(batchId);
        workOrder.setStatus(status);
        return workOrder;
    }

    private WorkStep createStep(WorkOrder workOrder, String stepName, WorkStepStatus status) {
        WorkStep step = new WorkStep();
        step.setId(UUID.randomUUID());
        step.setWorkOrder(workOrder);
        step.setStepName(stepName);
        step.setStatus(status);
        return step;
    }
}
