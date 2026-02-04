package com.merdeleine.production.controller;


import com.merdeleine.production.dto.BatchOrderLinkCreateRequest;
import com.merdeleine.production.dto.BatchOrderLinkResponse;
import com.merdeleine.production.dto.BatchOrderLinkUpdateRequest;
import com.merdeleine.production.mapper.BatchMappers;
import com.merdeleine.production.service.BatchOrderLinkService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/batches/{batchId}/order-links")
public class BatchOrderLinkController {

    private final BatchOrderLinkService service;

    public BatchOrderLinkController(BatchOrderLinkService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BatchOrderLinkResponse create(
            @PathVariable UUID batchId,
            @Valid @RequestBody BatchOrderLinkCreateRequest req
    ) {
        return BatchMappers.toOrderLinkResponse(service.create(batchId, req));
    }

    @GetMapping
    public List<BatchOrderLinkResponse> list(@PathVariable UUID batchId) {
        return service.list(batchId).stream().map(BatchMappers::toOrderLinkResponse).toList();
    }

    @GetMapping("/{linkId}")
    public BatchOrderLinkResponse get(@PathVariable UUID batchId, @PathVariable UUID linkId) {
        return BatchMappers.toOrderLinkResponse(service.get(batchId, linkId));
    }

    @PutMapping("/{linkId}")
    public BatchOrderLinkResponse update(
            @PathVariable UUID batchId,
            @PathVariable UUID linkId,
            @Valid @RequestBody BatchOrderLinkUpdateRequest req
    ) {
        return BatchMappers.toOrderLinkResponse(service.update(batchId, linkId, req));
    }

    @DeleteMapping("/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID batchId, @PathVariable UUID linkId) {
        service.delete(batchId, linkId);
    }
}
