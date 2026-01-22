package com.merdeleine.production.repository;

import com.merdeleine.production.entity.Batch;
import com.merdeleine.production.enums.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BatchRepository extends JpaRepository<Batch, UUID> {

    List<Batch> findBySellWindowId(UUID sellWindowId);

    List<Batch> findByProductId(UUID productId);

    List<Batch> findByStatus(BatchStatus status);

    @Query("SELECT b FROM Batch b WHERE b.sellWindowId = :sellWindowId AND b.productId = :productId")
    List<Batch> findBySellWindowIdAndProductId(@Param("sellWindowId") UUID sellWindowId, 
                                                @Param("productId") UUID productId);
}
