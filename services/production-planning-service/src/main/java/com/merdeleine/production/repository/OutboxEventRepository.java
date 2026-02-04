package com.merdeleine.production.repository;

import com.merdeleine.production.entity.OutboxEvent;
import com.merdeleine.production.enums.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop100ByStatusAndEventTypeOrderByCreatedAtAsc(
            OutboxEventStatus status,
            String eventType
    );
}
