package com.merdeleine.production.controller;


import com.merdeleine.production.dto.BatchScheduleResponse;
import com.merdeleine.production.dto.BatchScheduleUpsertRequest;
import com.merdeleine.production.mapper.BatchMappers;
import com.merdeleine.production.service.BatchScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/batches/{batchId}/schedule")
public class BatchScheduleController {

    private final BatchScheduleService service;

    public BatchScheduleController(BatchScheduleService service) {
        this.service = service;
    }

    @GetMapping
    public BatchScheduleResponse get(@PathVariable UUID batchId) {
        return BatchMappers.toScheduleResponse(service.getByBatchId(batchId));
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public BatchScheduleResponse upsert(
            @PathVariable UUID batchId,
            @Valid @RequestBody BatchScheduleUpsertRequest req
    ) {
        return BatchMappers.toScheduleResponse(service.upsert(batchId, req));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID batchId) {
        service.deleteByBatchId(batchId);
    }
}
