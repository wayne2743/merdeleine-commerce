package com.merdeleine.catalog.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.merdeleine.catalog.dto.PageResponse;
import com.merdeleine.catalog.dto.ProductSellWindowDto;
import com.merdeleine.catalog.service.ProductSellWindowService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/product-sell-windows")
public class ProductSellWindowController {

    private final ProductSellWindowService pswService;

    public ProductSellWindowController(ProductSellWindowService pswService) {
        this.pswService = pswService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductSellWindowDto.Response create(@Valid @RequestBody ProductSellWindowDto.CreateRequest req) throws JsonProcessingException {
        return pswService.create(req);
    }

    @PostMapping("/with-sell-window")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductSellWindowDto.Response createWithSellWindow(
            @Valid @RequestBody ProductSellWindowDto.CreateWithSellWindowRequest req
    ) throws JsonProcessingException {
        return pswService.createWithSellWindow(req);
    }

    @GetMapping("/{id}")
    public ProductSellWindowDto.Response get(@PathVariable UUID id) {
        return pswService.get(id);
    }

    @GetMapping
    public List<ProductSellWindowDto.Response> list(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID sellWindowId
    ) {
        return pswService.list(productId, sellWindowId);
    }

    @GetMapping("/page")
    public PageResponse<ProductSellWindowDto.PageItem> page(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID sellWindowId,
            Pageable pageable
    ) {
        return pswService.page(productId, sellWindowId, pageable);
    }

    @PutMapping("/{id}")
    public ProductSellWindowDto.Response update(@PathVariable UUID id,
                                                @Valid @RequestBody ProductSellWindowDto.UpdateRequest req) {
        return pswService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        pswService.delete(id);
    }
}
