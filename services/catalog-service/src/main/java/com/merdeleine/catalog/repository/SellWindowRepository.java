package com.merdeleine.catalog.repository;


import com.merdeleine.catalog.entity.SellWindow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SellWindowRepository extends JpaRepository<SellWindow, UUID> {
    Optional<SellWindow> findByName(String name);
    boolean existsByName(String name);
}
