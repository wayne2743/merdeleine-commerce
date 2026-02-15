package com.merdeleine.notification.repository;

import com.merdeleine.notification.entity.NotificationJob;
import com.merdeleine.notification.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface NotificationJobRepository extends JpaRepository<NotificationJob, UUID> {
    List<NotificationJob> findTop100ByStatusOrderByCreatedAtAsc(NotificationStatus status);

    @Query(value = """
        select exists(
            select 1
            from notification_job nj
            where nj.channel = :channel
              and nj.template_key = :templateKey
              and (nj.payload ->> 'paymentId') = :paymentId
        )
        """, nativeQuery = true)
    boolean existsByPaymentIdAndTemplateKeyAndChannel(String paymentId,
                                                      String templateKey,
                                                      String channel);
}
