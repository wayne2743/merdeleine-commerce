package com.merdeleine.production.service;


import com.merdeleine.production.dto.BatchCreateRequest;
import com.merdeleine.production.dto.BatchUpdateRequest;
import com.merdeleine.production.entity.Batch;
import com.merdeleine.production.repository.BatchRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BatchService {

    private final BatchRepository batchRepository;

    public BatchService(BatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    @Transactional
    public Batch create(BatchCreateRequest req) {
        Batch b = new Batch();
        b.setId(UUID.randomUUID());
        b.setSellWindowId(req.getSellWindowId());
        b.setProductId(req.getProductId());
        b.setTargetQty(req.getTargetQty());
        b.setStatus(req.getStatus());
        b.setConfirmedAt(req.getConfirmedAt());
        return batchRepository.save(b);
    }

    @Transactional(readOnly = true)
    public Batch get(UUID id) {
        return batchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Batch not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Batch> list(Pageable pageable) {
        return batchRepository.findAll(pageable);
    }

    @Transactional
    public Batch update(UUID id, BatchUpdateRequest req) {
        Batch b = get(id);

        if (req.getTargetQty() != null) b.setTargetQty(req.getTargetQty());
        if (req.getStatus() != null) b.setStatus(req.getStatus());
        if (req.getConfirmedAt() != null) b.setConfirmedAt(req.getConfirmedAt());

        return batchRepository.save(b);
    }

    @Transactional
    public void delete(UUID id) {
        if (!batchRepository.existsById(id)) {
            throw new EntityNotFoundException("Batch not found: " + id);
        }
        batchRepository.deleteById(id);
    }
}
