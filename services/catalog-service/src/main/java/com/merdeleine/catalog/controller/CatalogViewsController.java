package com.merdeleine.catalog.controller;

import com.merdeleine.catalog.client.OrderQuotaClient;
import com.merdeleine.catalog.dto.PageResponse;
import com.merdeleine.catalog.dto.ProductSellWindowView;
import com.merdeleine.catalog.repository.ProductSellWindowRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/views")
public class CatalogViewsController {

    private final ProductSellWindowRepository pswRepo;
    private final OrderQuotaClient orderQuotaClient;

    public CatalogViewsController(ProductSellWindowRepository pswRepo, OrderQuotaClient orderQuotaClient) {
        this.pswRepo = pswRepo;
        this.orderQuotaClient = orderQuotaClient;
    }

    @GetMapping("/product-sell-windows")
    public PageResponse<ProductSellWindowView> pageProductSellWindows(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);

        var rowsPage = pswRepo.pageRows(PageRequest.of(safePage, safeSize));
        var rows = rowsPage.getContent();

        List<OrderQuotaClient.Key> keys = rows.stream()
                .map(r -> new OrderQuotaClient.Key(r.getSellWindowId(), r.getProductId()))
                .distinct()
                .toList();

        List<OrderQuotaClient.QuotaDto> quotas;
        try {
            quotas = orderQuotaClient.batchGet(keys);
        } catch (Exception ex) {
            quotas = List.of();
        }

        Map<String, OrderQuotaClient.QuotaDto> quotaMap = quotas.stream()
                .collect(Collectors.toMap(
                        q -> keyOf(q.sellWindowId(), q.productId()),
                        q -> q,
                        (a, b) -> a
                ));

        List<ProductSellWindowView> items = rows.stream()
                .map(r -> {
                    var q = quotaMap.get(keyOf(r.getSellWindowId(), r.getProductId()));
                    return toView(r, q);
                })
                .toList();

        return new PageResponse<>(
                items,
                safePage,
                safeSize,
                rowsPage.getTotalElements()
        );
    }

    @GetMapping("/product-sell-windows/{productSellWindowId}")
    public ProductSellWindowView getProductSellWindow(@PathVariable UUID productSellWindowId) {
        var r = pswRepo.findRowByProductSellWindowId(productSellWindowId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "ProductSellWindow not found: " + productSellWindowId
                ));

        List<OrderQuotaClient.Key> keys = List.of(new OrderQuotaClient.Key(r.getSellWindowId(), r.getProductId()));

        List<OrderQuotaClient.QuotaDto> quotas;
        try {
            quotas = orderQuotaClient.batchGet(keys);
        } catch (Exception ex) {
            quotas = List.of();
        }

        var q = quotas.stream().findFirst().orElse(null);
        return toView(r, q);
    }

    private ProductSellWindowView toView(
            ProductSellWindowRepository.ProductSellWindowRow r,
            OrderQuotaClient.QuotaDto q
    ) {
        int soldQty = (q != null && q.soldQty() != null) ? q.soldQty() : 0;
        String status = (q != null && q.status() != null) ? q.status() : "OPEN";

        return new ProductSellWindowView(
                r.getProductSellWindowId(),

                r.getSellWindowId(),
                r.getSellWindowName(),
                r.getStartAt(),
                r.getEndAt(),
                r.getTimezone(),
                r.getPaymentCloseAt(),

                r.getProductId(),
                r.getProductName(),
                r.getUnitPriceCents(),
                r.getCurrency(),

                r.getMinQty(),
                r.getMaxQty(),

                soldQty,
                status,
                (q != null) ? q.updatedAt() : null
        );
    }


    private static String keyOf(UUID sellWindowId, UUID productId) {
        return sellWindowId + ":" + productId;
    }
}