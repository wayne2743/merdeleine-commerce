package com.merdeleine.production.repository;

import com.merdeleine.production.entity.BatchSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BatchScheduleRepository extends JpaRepository<BatchSchedule, UUID> {

    Optional<BatchSchedule> findByBatchId(UUID batchId);
}
