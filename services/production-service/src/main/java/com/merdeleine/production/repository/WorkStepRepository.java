package com.merdeleine.production.repository;

import com.merdeleine.production.entity.WorkStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkStepRepository extends JpaRepository<WorkStep, UUID> {

    List<WorkStep> findByWorkOrderId(UUID workOrderId);


    List<WorkStep> findByWorkOrder_Id(UUID workOrderId);

    void deleteByWorkOrder_Id(UUID workOrderId);
}
