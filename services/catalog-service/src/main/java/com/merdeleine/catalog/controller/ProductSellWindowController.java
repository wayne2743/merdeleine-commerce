package com.merdeleine.catalog.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.merdeleine.catalog.dto.ProductSellWindowDto;
import com.merdeleine.catalog.service.ProductSellWindowService;
import jakarta.validation.Valid;
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
