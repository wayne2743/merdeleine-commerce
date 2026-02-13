package com.merdeleine.production.planning.controller;


import com.merdeleine.production.planning.dto.BatchCreateRequest;
import com.merdeleine.production.planning.dto.BatchResponse;
import com.merdeleine.production.planning.dto.BatchUpdateRequest;
import com.merdeleine.production.planning.mapper.BatchMappers;
import com.merdeleine.production.planning.service.BatchService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/batches")
public class BatchController {

    private final BatchService service;

    public BatchController(BatchService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BatchResponse create(@Valid @RequestBody BatchCreateRequest req) {
        return BatchMappers.toBatchResponse(service.create(req));
    }

    @GetMapping("/{id}")
    public BatchResponse get(@PathVariable UUID id) {
        return BatchMappers.toBatchResponse(service.get(id));
    }

    @GetMapping
    public Page<BatchResponse> list(Pageable pageable) {
        return service.list(pageable).map(BatchMappers::toBatchResponse);
    }

    @PutMapping("/{id}")
    public BatchResponse update(@PathVariable UUID id, @Valid @RequestBody BatchUpdateRequest req) {
        return BatchMappers.toBatchResponse(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }


    @PostMapping("/{id}/confirm")
    public BatchResponse confirm(@PathVariable UUID id) {
        return BatchMappers.toBatchResponse(service.confirm(id));
    }
}
