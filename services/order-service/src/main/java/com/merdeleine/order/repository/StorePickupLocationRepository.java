package com.merdeleine.order.repository;

import com.merdeleine.order.entity.StorePickupLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StorePickupLocationRepository extends JpaRepository<StorePickupLocation, UUID> {
    List<StorePickupLocation> findByActiveTrueOrderByCreatedAtDesc();
}

