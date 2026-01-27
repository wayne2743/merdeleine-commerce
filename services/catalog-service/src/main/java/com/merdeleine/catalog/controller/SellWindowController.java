package com.merdeleine.catalog.controller;

import com.merdeleine.catalog.dto.SellWindowDto;
import com.merdeleine.catalog.service.SellWindowService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sell-windows")
public class SellWindowController {

    private final SellWindowService sellWindowService;

    public SellWindowController(SellWindowService sellWindowService) {
        this.sellWindowService = sellWindowService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SellWindowDto.Response create(@Valid @RequestBody SellWindowDto.CreateRequest req) {
        return sellWindowService.create(req);
    }

    @GetMapping("/{id}")
    public SellWindowDto.Response get(@PathVariable UUID id) {
        return sellWindowService.get(id);
    }

    @GetMapping
    public List<SellWindowDto.Response> list() {
        return sellWindowService.list();
    }

    @PutMapping("/{id}")
    public SellWindowDto.Response update(@PathVariable UUID id,
                                         @Valid @RequestBody SellWindowDto.UpdateRequest req) {
        return sellWindowService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        sellWindowService.delete(id);
    }
}
