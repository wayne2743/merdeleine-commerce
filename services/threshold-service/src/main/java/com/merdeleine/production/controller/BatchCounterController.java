package com.merdeleine.production.controller;

import com.merdeleine.production.dto.BatchCounterDto;
import com.merdeleine.production.dto.CounterEventLogDto;
import com.merdeleine.production.service.BatchCounterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/batch-counters")
public class BatchCounterController {

    private final BatchCounterService batchCounterService;

    public BatchCounterController(BatchCounterService batchCounterService) {
        this.batchCounterService = batchCounterService;
    }

    // Create
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BatchCounterDto.Response create(@Valid @RequestBody BatchCounterDto.CreateRequest req) {
        return batchCounterService.create(req);
    }

    // Read
    @GetMapping("/{id}")
    public BatchCounterDto.Response get(@PathVariable UUID id) {
        return batchCounterService.get(id);
    }

    // List (optional filter)
    @GetMapping
    public List<BatchCounterDto.Response> list(
            @RequestParam(required = false) UUID sellWindowId,
            @RequestParam(required = false) UUID productId
    ) {
        return batchCounterService.list(sellWindowId, productId);
    }

    // Update
    @PutMapping("/{id}")
    public BatchCounterDto.Response update(@PathVariable UUID id,
                                           @Valid @RequestBody BatchCounterDto.UpdateRequest req) {
        return batchCounterService.update(id, req);
    }

    // Delete
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        batchCounterService.delete(id);
    }

    // Apply paid event (increase paidQty + write CounterEventLog)
    @PostMapping("/{id}/paid-events")
    public BatchCounterDto.Response applyPaidEvent(@PathVariable UUID id,
                                                   @Valid @RequestBody BatchCounterDto.ApplyPaidEventRequest req) {
        return batchCounterService.applyPaidEvent(id, req);
    }

    // List event logs
    @GetMapping("/{id}/event-logs")
    public List<CounterEventLogDto.Response> listEventLogs(@PathVariable UUID id) {
        return batchCounterService.listEventLogs(id);
    }
}
