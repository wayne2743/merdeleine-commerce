package com.merdeleine.order.service;

import com.merdeleine.order.dto.StorePickupLocationRequest;
import com.merdeleine.order.dto.StorePickupLocationResponse;
import com.merdeleine.order.entity.StorePickupLocation;
import com.merdeleine.order.exception.ResourceNotFoundException;
import com.merdeleine.order.repository.StorePickupLocationRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class StorePickupLocationService {

    private final StorePickupLocationRepository repository;

    public StorePickupLocationService(StorePickupLocationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public StorePickupLocationResponse create(StorePickupLocationRequest req) {
        StorePickupLocation location = new StorePickupLocation();
        location.setName(req.name());
        location.setAddress(req.address());
        location.setContactPhone(req.contactPhone());
        location.setActive(req.active() == null || req.active());
        StorePickupLocation saved = repository.save(location);
        return toResponse(saved);
    }

    @Transactional
    public StorePickupLocationResponse get(UUID id) {
        return toResponse(find(id));
    }

    @Transactional
    public List<StorePickupLocationResponse> listAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<StorePickupLocationResponse> listActive() {
        return repository.findByActiveTrueOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public StorePickupLocationResponse update(UUID id, StorePickupLocationRequest req) {
        StorePickupLocation location = find(id);
        location.setName(req.name());
        location.setAddress(req.address());
        location.setContactPhone(req.contactPhone());
        if (req.active() != null) {
            location.setActive(req.active());
        }
        return toResponse(repository.save(location));
    }

    @Transactional
    public void delete(UUID id) {
        StorePickupLocation location = find(id);
        repository.delete(location);
    }

    private StorePickupLocation find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Store pickup location not found: " + id));
    }

    private StorePickupLocationResponse toResponse(StorePickupLocation location) {
        return new StorePickupLocationResponse(
                location.getId(),
                location.getName(),
                location.getAddress(),
                location.getContactPhone(),
                location.isActive(),
                location.getCreatedAt(),
                location.getUpdatedAt()
        );
    }
}

