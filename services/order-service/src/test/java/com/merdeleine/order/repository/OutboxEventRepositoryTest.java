package com.merdeleine.order.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.merdeleine.order.entity.OutboxEvent;
import com.merdeleine.order.enums.OutboxEventStatus;
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
            .withDatabaseName("order_db")
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
    private OutboxEventRepository outboxEventRepository;

    private UUID aggregateId;
    private OutboxEvent testEvent;

    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        aggregateId = UUID.randomUUID();
        
        testEvent = new OutboxEvent();
        testEvent.setId(UUID.randomUUID());
        testEvent.setAggregateType("ORDER");
        testEvent.setAggregateId(aggregateId);
        testEvent.setEventType("order.created.v1");

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("orderId", aggregateId.toString());
        testEvent.setPayload(payload);
        testEvent.setStatus(OutboxEventStatus.NEW);
    }

    @Test
    void testSaveOutboxEvent() {
        OutboxEvent saved = outboxEventRepository.saveAndFlush(testEvent);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.NEW);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getSentAt()).isNull();
    }

    @Test
    void testFindByStatus() {
        entityManager.persistAndFlush(testEvent);
        
        OutboxEvent sentEvent = createEvent("order.paid.v1", OutboxEventStatus.SENT);
        entityManager.persistAndFlush(sentEvent);
        
        List<OutboxEvent> newEvents = outboxEventRepository.findByStatus(OutboxEventStatus.NEW);
        
        assertThat(newEvents).hasSize(1);
        assertThat(newEvents.get(0).getStatus()).isEqualTo(OutboxEventStatus.NEW);
    }

    @Test
    void testFindByAggregateTypeAndAggregateId() {
        entityManager.persistAndFlush(testEvent);
        
        List<OutboxEvent> events = outboxEventRepository.findByAggregateTypeAndAggregateId("ORDER", aggregateId);
        
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getAggregateId()).isEqualTo(aggregateId);
    }

    @Test
    void testFindByStatusOrderByCreatedAtAsc() {
        OutboxEvent event1 = createEvent("order.created.v1", OutboxEventStatus.NEW);
        OutboxEvent event2 = createEvent("order.updated.v1", OutboxEventStatus.NEW);
        
        entityManager.persistAndFlush(event1);
        // Small delay to ensure different timestamps
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        entityManager.persistAndFlush(event2);
        
        List<OutboxEvent> events = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.NEW);
        
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getCreatedAt()).isBeforeOrEqualTo(events.get(1).getCreatedAt());
    }

    @Test
    void testUpdateEventStatus() {
        OutboxEvent saved = entityManager.persistAndFlush(testEvent);
        saved.setStatus(OutboxEventStatus.SENT);
        saved.setSentAt(OffsetDateTime.now());
        
        OutboxEvent updated = outboxEventRepository.save(saved);
        
        assertThat(updated.getStatus()).isEqualTo(OutboxEventStatus.SENT);
        assertThat(updated.getSentAt()).isNotNull();
    }

    @Test
    void testFindPendingEventsBefore() {
        OutboxEvent event = createEvent("order.created.v1", OutboxEventStatus.NEW);
        entityManager.persistAndFlush(event);
        
        OffsetDateTime before = OffsetDateTime.now().plusMinutes(1);
        List<OutboxEvent> events = outboxEventRepository.findPendingEventsBefore(OutboxEventStatus.NEW, before);
        
        assertThat(events).hasSize(1);
    }

    private OutboxEvent createEvent(String eventType, OutboxEventStatus status) {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateType("ORDER");
        event.setAggregateId(UUID.randomUUID());
        event.setEventType(eventType);
        event.setPayload(objectMapper.createObjectNode());
        event.setStatus(status);
        return event;
    }
}
