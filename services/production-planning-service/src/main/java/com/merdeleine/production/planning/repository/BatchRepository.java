package com.merdeleine.production.planning.repository;

import com.merdeleine.production.planning.entity.Batch;
import com.merdeleine.production.planning.enums.BatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BatchRepository extends JpaRepository<Batch, UUID> {
    Page<Batch> findBySellWindowId(UUID sellWindowId, Pageable pageable);
    Page<Batch> findByProductId(UUID productId, Pageable pageable);
    Page<Batch> findByStatus(BatchStatus status, Pageable pageable);

    Optional<Batch> findByProductIdAndSellWindowId(UUID productId, UUID sellWindowId);
}
