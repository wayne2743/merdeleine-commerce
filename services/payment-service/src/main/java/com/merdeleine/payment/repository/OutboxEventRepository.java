package com.merdeleine.payment.repository;

import com.merdeleine.payment.entity.OutboxEvent;
import com.merdeleine.payment.enums.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByStatus(OutboxEventStatus status);

    List<OutboxEvent> findByAggregateTypeAndAggregateId(String aggregateType, UUID aggregateId);

    @Query("SELECT oe FROM OutboxEvent oe WHERE oe.status = :status ORDER BY oe.createdAt ASC")
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(@Param("status") OutboxEventStatus status);

    @Query("SELECT oe FROM OutboxEvent oe WHERE oe.status = :status AND oe.createdAt <= :before ORDER BY oe.createdAt ASC")
    List<OutboxEvent> findPendingEventsBefore(@Param("status") OutboxEventStatus status, 
                                               @Param("before") OffsetDateTime before);

    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus outboxEventStatus);
}
