package com.merdeleine.catalog.controller;

import com.merdeleine.catalog.client.OrderQuotaClient;
import com.merdeleine.catalog.dto.PageResponse;
import com.merdeleine.catalog.dto.ProductSellWindowView;
import com.merdeleine.catalog.repository.ProductSellWindowRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/catalog/views")
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
        // 防炸：限制 size
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);

        var rowsPage = pswRepo.pageRows(PageRequest.of(safePage, safeSize));
        var rows = rowsPage.getContent();

        // keys
        List<OrderQuotaClient.Key> keys = rows.stream()
                .map(r -> new OrderQuotaClient.Key(r.sellWindowId(), r.productId()))
                .distinct()
                .toList();

        // batch quotas（order service 掛了怎麼辦？先用降級策略）
        List<OrderQuotaClient.QuotaDto> quotas;
        try {
            quotas = orderQuotaClient.batchGet(keys);
        } catch (Exception ex) {
            // 降級：quota 全部當 0 / OPEN（你也可以改成直接回 502）
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
                    var q = quotaMap.get(keyOf(r.sellWindowId(), r.productId()));
                    int soldQty = (q != null && q.soldQty() != null) ? q.soldQty() : 0;
                    String status = (q != null && q.status() != null) ? q.status() : "OPEN";

                    return new ProductSellWindowView(
                            r.sellWindowId(),
                            r.sellWindowName(),
                            r.startAt(),
                            r.endAt(),
                            r.timezone(),
                            r.paymentCloseAt(),

                            r.productId(),
                            r.productName(),

                            r.minQty(),
                            r.maxQty(),

                            soldQty,
                            status,
                            (q != null) ? q.updatedAt() : null
                    );
                })
                .toList();

        return new PageResponse<>(
                items,
                safePage,
                safeSize,
                rowsPage.getTotalElements()
        );
    }

    private static String keyOf(UUID sellWindowId, UUID productId) {
        return sellWindowId + ":" + productId;
    }
}