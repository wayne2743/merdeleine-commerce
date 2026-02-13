package com.merdeleine.production.repository;

import com.merdeleine.production.entity.WorkOrder;
import com.merdeleine.production.enums.WorkOrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {

     List<WorkOrder> findByBatchId(UUID batchId);

    List<WorkOrder> findByStatus(WorkOrderStatus status);

    @EntityGraph(attributePaths = "steps")
    Optional<WorkOrder> findWithStepsById(UUID id);
}
