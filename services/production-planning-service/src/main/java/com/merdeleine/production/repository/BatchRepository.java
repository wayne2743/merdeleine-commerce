package com.merdeleine.production.repository;

import com.merdeleine.production.entity.Batch;
import com.merdeleine.production.enums.BatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BatchRepository extends JpaRepository<Batch, UUID> {
    Page<Batch> findBySellWindowId(UUID sellWindowId, Pageable pageable);
    Page<Batch> findByProductId(UUID productId, Pageable pageable);
    Page<Batch> findByStatus(BatchStatus status, Pageable pageable);
}
