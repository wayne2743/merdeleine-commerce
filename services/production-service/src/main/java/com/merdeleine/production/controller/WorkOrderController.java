package com.merdeleine.production.controller;

import com.merdeleine.production.dto.WorkOrderDtos.*;
import com.merdeleine.production.service.WorkOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkOrderResponse create(@Valid @RequestBody CreateWorkOrderRequest req) {
        return workOrderService.create(req);
    }

    @GetMapping("/{id}")
    public WorkOrderResponse get(@PathVariable UUID id) {
        return workOrderService.get(id);
    }

    @GetMapping
    public List<WorkOrderResponse> list(@RequestParam(required = false) UUID batchId) {
        return workOrderService.list(Optional.ofNullable(batchId));
    }

    @PutMapping("/{id}")
    public WorkOrderResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateWorkOrderRequest req) {
        return workOrderService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        workOrderService.delete(id);
    }
}
