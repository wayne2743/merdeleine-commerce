package com.merdeleine.production.planning.repository;

import com.merdeleine.production.planning.entity.BatchSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BatchScheduleRepository extends JpaRepository<BatchSchedule, UUID> {
    Optional<BatchSchedule> findByBatch_Id(UUID batchId);
    boolean existsByBatch_Id(UUID batchId);
    void deleteByBatch_Id(UUID batchId);
}
