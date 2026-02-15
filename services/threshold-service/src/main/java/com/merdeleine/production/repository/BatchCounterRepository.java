package com.merdeleine.production.repository;

import com.merdeleine.production.entity.BatchCounter;
import com.merdeleine.production.enums.CounterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BatchCounterRepository extends JpaRepository<BatchCounter, UUID> {

    Optional<BatchCounter> findBySellWindowIdAndProductId(UUID sellWindowId, UUID productId);

    List<BatchCounter> findBySellWindowId(UUID sellWindowId);

    List<BatchCounter> findByProductId(UUID productId);

    List<BatchCounter> findByStatus(CounterStatus status);

    boolean existsBySellWindowIdAndProductId(UUID sellWindowId, UUID productId);
}
