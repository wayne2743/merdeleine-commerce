package com.merdeleine.production.planning.service;


import com.merdeleine.production.planning.dto.BatchScheduleUpsertRequest;
import com.merdeleine.production.planning.entity.Batch;
import com.merdeleine.production.planning.entity.BatchSchedule;
import com.merdeleine.production.planning.repository.BatchScheduleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BatchScheduleService {

    private final BatchService batchService;
    private final BatchScheduleRepository repo;

    public BatchScheduleService(BatchService batchService, BatchScheduleRepository repo) {
        this.batchService = batchService;
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public BatchSchedule getByBatchId(UUID batchId) {
        batchService.get(batchId);
        return repo.findByBatch_Id(batchId)
                .orElseThrow(() -> new EntityNotFoundException("BatchSchedule not found: batchId=" + batchId));
    }

    @Transactional
    public BatchSchedule upsert(UUID batchId, BatchScheduleUpsertRequest req) {
        Batch batch = batchService.get(batchId);

        BatchSchedule schedule = repo.findByBatch_Id(batchId).orElse(null);
        if (schedule == null) {
            schedule = new BatchSchedule();
            schedule.setId(UUID.randomUUID());
            schedule.setBatch(batch);
        }

        schedule.setPlannedProductionDate(req.getPlannedProductionDate());
        schedule.setPlannedShipDate(req.getPlannedShipDate());
        schedule.setNotes(req.getNotes());

        return repo.save(schedule);
    }

    @Transactional
    public void deleteByBatchId(UUID batchId) {
        if (!repo.existsByBatch_Id(batchId)) {
            throw new EntityNotFoundException("BatchSchedule not found: batchId=" + batchId);
        }
        repo.deleteByBatch_Id(batchId);
    }
}
