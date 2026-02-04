package com.merdeleine.production.repository;

import com.merdeleine.production.entity.BatchOrderLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BatchOrderLinkRepository extends JpaRepository<BatchOrderLink, UUID> {
    List<BatchOrderLink> findByBatch_Id(UUID batchId);
    Optional<BatchOrderLink> findByIdAndBatch_Id(UUID id, UUID batchId);
    void deleteByIdAndBatch_Id(UUID id, UUID batchId);
    boolean existsByIdAndBatch_Id(UUID id, UUID batchId);
}
