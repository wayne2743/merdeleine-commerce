package com.merdeleine.production.repository;

import com.merdeleine.production.entity.CounterEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CounterEventLogRepository extends JpaRepository<CounterEventLog, UUID> {

    List<CounterEventLog> findByCounterId(UUID counterId);

    @Query("SELECT cel FROM CounterEventLog cel WHERE cel.sourceEventId = :sourceEventId")
    List<CounterEventLog> findBySourceEventId(@Param("sourceEventId") UUID sourceEventId);

    boolean existsBySourceEventId(UUID sourceEventId);

    List<CounterEventLog> findByCounter_IdOrderByCreatedAtAsc(UUID counterId);
}
