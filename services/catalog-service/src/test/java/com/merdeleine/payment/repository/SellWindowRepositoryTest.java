package com.merdeleine.payment.repository;


import com.merdeleine.catalog.entity.SellWindow;
import com.merdeleine.catalog.repository.SellWindowRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class SellWindowRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("catalog_db")
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
    private SellWindowRepository sellWindowRepository;

    private SellWindow testSellWindow;

    @BeforeEach
    void setUp() {
        testSellWindow = new SellWindow();
        testSellWindow.setId(UUID.randomUUID());
        testSellWindow.setName("Spring 2024");
        testSellWindow.setStartAt(OffsetDateTime.now());
        testSellWindow.setEndAt(OffsetDateTime.now().plusMonths(3));
        testSellWindow.setTimezone("Asia/Taipei");
    }

    @Test
    void testSaveSellWindow() {
        SellWindow saved = sellWindowRepository.saveAndFlush(testSellWindow);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Spring 2024");
        assertThat(saved.getStartAt()).isNotNull();
        assertThat(saved.getEndAt()).isNotNull();
        assertThat(saved.getTimezone()).isEqualTo("Asia/Taipei");
    }

    @Test
    void testFindByName() {
        entityManager.persistAndFlush(testSellWindow);
        
        Optional<SellWindow> found = sellWindowRepository.findByName("Spring 2024");
        
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Spring 2024");
    }

    @Test
    void testUpdateSellWindow() {
        SellWindow saved = entityManager.persistAndFlush(testSellWindow);
        saved.setName("Summer 2024");
        saved.setEndAt(OffsetDateTime.now().plusMonths(6));
        
        SellWindow updated = sellWindowRepository.saveAndFlush(saved);
        
        assertThat(updated.getName()).isEqualTo("Summer 2024");
        assertThat(updated.getEndAt()).isAfter(saved.getStartAt());
    }

    @Test
    void testDeleteSellWindow() {
        SellWindow saved = entityManager.persistAndFlush(testSellWindow);
        UUID sellWindowId = saved.getId();
        
        sellWindowRepository.delete(saved);
        entityManager.flush();
        
        assertThat(sellWindowRepository.findById(sellWindowId)).isEmpty();
    }
}
