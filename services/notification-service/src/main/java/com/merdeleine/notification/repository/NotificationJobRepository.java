package com.merdeleine.notification.repository;

import com.merdeleine.notification.entity.NotificationJob;
import com.merdeleine.notification.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationJobRepository extends JpaRepository<NotificationJob, UUID> {
    List<NotificationJob> findTop100ByStatusOrderByCreatedAtAsc(NotificationStatus status);
}
