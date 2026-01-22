package com.merdeleine.production.repository;

import com.merdeleine.production.entity.BatchOrderLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BatchOrderLinkRepository extends JpaRepository<BatchOrderLink, UUID> {

    List<BatchOrderLink> findByBatchId(UUID batchId);

    List<BatchOrderLink> findByOrderId(UUID orderId);
}
