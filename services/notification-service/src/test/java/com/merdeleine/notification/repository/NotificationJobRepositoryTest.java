package com.merdeleine.notification.repository;

import com.merdeleine.notification.entity.NotificationJob;
import com.merdeleine.notification.enums.NotificationChannel;
import com.merdeleine.notification.enums.NotificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class NotificationJobRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("notification_db")
            .withUsername("merdeleine")
            .withPassword("merdeleine_pw");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NotificationJobRepository notificationJobRepository;

    private NotificationJob requestedJob;

    @BeforeEach
    void setUp() {
        requestedJob = new NotificationJob();
        requestedJob.setId(UUID.randomUUID());
        requestedJob.setChannel(NotificationChannel.EMAIL);
        requestedJob.setRecipient("test@example.com");
        requestedJob.setTemplateKey("order.confirmed");
        requestedJob.setPayload(Map.of("orderId", UUID.randomUUID().toString()));
        requestedJob.setStatus(NotificationStatus.REQUESTED);
        requestedJob.setRetryCount(0);
    }

    @Test
    void testSaveNotificationJob() {
        NotificationJob saved = notificationJobRepository.saveAndFlush(requestedJob);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(saved.getRecipient()).isEqualTo("test@example.com");
        assertThat(saved.getTemplateKey()).isEqualTo("order.confirmed");
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.REQUESTED);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getSentAt()).isNull();
    }

    @Test
    void testFindTop100ByStatusOrderByCreatedAtAsc() {
        entityManager.persistAndFlush(requestedJob);

        NotificationJob sentJob = new NotificationJob();
        sentJob.setId(UUID.randomUUID());
        sentJob.setChannel(NotificationChannel.SMS);
        sentJob.setRecipient("0912345678");
        sentJob.setTemplateKey("order.shipped");
        sentJob.setPayload(Map.of("trackingNo", "TRK-" + UUID.randomUUID()));
        sentJob.setStatus(NotificationStatus.SENT);
        entityManager.persistAndFlush(sentJob);

        List<NotificationJob> requested = notificationJobRepository
                .findTop100ByStatusOrderByCreatedAtAsc(NotificationStatus.REQUESTED);

        assertThat(requested).hasSize(1);
        assertThat(requested.get(0).getStatus()).isEqualTo(NotificationStatus.REQUESTED);
    }
}
