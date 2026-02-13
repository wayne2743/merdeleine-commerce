package com.merdeleine.payment.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.payment.entity.OutboxEvent;
import com.merdeleine.payment.enums.OutboxEventStatus;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class OutboxEventRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("payment_db")
            .withUsername("merdeleine")
            .withPassword("merdeleine_pw");
    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private UUID paymentId;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID();
    }

    @Test
    void testSaveOutboxEvent() {
        OutboxEvent event = createEvent("payment.created.v1", OutboxEventStatus.NEW);
        OutboxEvent saved = outboxEventRepository.save(event);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAggregateType()).isEqualTo("PAYMENT");
        assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.NEW);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void testFindByStatus() {
        OutboxEvent newEvent = createEvent("payment.created.v1", OutboxEventStatus.NEW);
        OutboxEvent sentEvent = createEvent("payment.succeeded.v1", OutboxEventStatus.SENT);
        
        entityManager.persistAndFlush(newEvent);
        entityManager.persistAndFlush(sentEvent);
        
        List<OutboxEvent> newEvents = outboxEventRepository.findByStatus(OutboxEventStatus.NEW);
        
        assertThat(newEvents).hasSize(1);
        assertThat(newEvents.get(0).getStatus()).isEqualTo(OutboxEventStatus.NEW);
    }

    @Test
    void testFindByAggregateTypeAndAggregateId() {
        OutboxEvent event = createEvent("payment.created.v1", OutboxEventStatus.NEW);
        entityManager.persistAndFlush(event);
        
        List<OutboxEvent> events = outboxEventRepository.findByAggregateTypeAndAggregateId("PAYMENT", paymentId);
        
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getAggregateId()).isEqualTo(paymentId);
    }

    @Test
    void testFindByStatusOrderByCreatedAtAsc() {
        OutboxEvent event1 = createEvent("payment.created.v1", OutboxEventStatus.NEW);
        OutboxEvent event2 = createEvent("payment.updated.v1", OutboxEventStatus.NEW);
        
        entityManager.persistAndFlush(event1);
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        entityManager.persistAndFlush(event2);
        
        List<OutboxEvent> events = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.NEW);
        
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getCreatedAt()).isBeforeOrEqualTo(events.get(1).getCreatedAt());
    }

    @Test
    void testUpdateEventStatus() {
        OutboxEvent event = createEvent("payment.created.v1", OutboxEventStatus.NEW);
        OutboxEvent saved = entityManager.persistAndFlush(event);
        
        saved.setStatus(OutboxEventStatus.SENT);
        saved.setSentAt(OffsetDateTime.now());
        OutboxEvent updated = outboxEventRepository.save(saved);
        
        assertThat(updated.getStatus()).isEqualTo(OutboxEventStatus.SENT);
        assertThat(updated.getSentAt()).isNotNull();
    }

    private OutboxEvent createEvent(String eventType, OutboxEventStatus status) {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateType("PAYMENT");
        event.setAggregateId(paymentId);
        event.setEventType(eventType);
        event.setPayload(null);
        event.setStatus(status);
        return event;
    }
}
