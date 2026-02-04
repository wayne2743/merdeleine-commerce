package com.merdeleine.catalog.repository;

import com.merdeleine.catalog.entity.OutboxEvent;
import com.merdeleine.catalog.enums.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus status);
}
