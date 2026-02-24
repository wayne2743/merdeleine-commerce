package com.merdeleine.order.service;


import com.merdeleine.order.dto.CloseQuotaDtos;
import com.merdeleine.order.dto.SellWindowQuotaBatchDto;
import com.merdeleine.order.dto.SellWindowQuotaUpsertDtos;
import com.merdeleine.order.entity.SellWindowQuota;
import com.merdeleine.order.repository.SellWindowQuotaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SellWindowQuotaService {

    public static final String STATUS_CLOSED = "CLOSED";
    private static final String STATUS_OPEN = "OPEN";

    private final SellWindowQuotaRepository repo;


    public SellWindowQuotaService(SellWindowQuotaRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public CloseQuotaDtos.CloseQuotaResponse close(CloseQuotaDtos.CloseQuotaRequest req) {
        // 若你希望 quota 不存在就 404，可以做 exists check（多一次 query）
        // 也可以不查，直接 close 回 0 表示沒更新（但無法區分已關 vs 不存在）
        boolean exists = repo.existsBySellWindowIdAndProductId(req.sellWindowId(), req.productId());
        if (!exists) {
            throw new IllegalArgumentException("sell_window_quota not found");
        }

        int updated = repo.close(req.sellWindowId(), req.productId(), STATUS_CLOSED, OffsetDateTime.now());
        return new CloseQuotaDtos.CloseQuotaResponse(req.sellWindowId(), req.productId(), updated == 1, STATUS_CLOSED);
    }


    @Transactional(readOnly = true)
    public List<SellWindowQuotaBatchDto.QuotaResponse> batchGet(
            List<SellWindowQuotaBatchDto.Key> keys
    ) {
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }

        List<SellWindowQuota> rows = repo.findByKeys(keys);

        return rows.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public SellWindowQuotaUpsertDtos.Response upsert(SellWindowQuotaUpsertDtos.Request req) {

        // 用 for update 避免併發下同 key 同時 insert
        SellWindowQuota q = repo.findForUpdate(req.sellWindowId(), req.productId())
                .orElseGet(() -> {
                    SellWindowQuota created = new SellWindowQuota();
                    created.setId(UUID.randomUUID());
                    created.setSellWindowId(req.sellWindowId());
                    created.setProductId(req.productId());
                    created.setSoldQty(0);
                    created.setStatus(STATUS_OPEN);
                    return created;
                });

        // 更新可變欄位（min/max）
        q.setMinQty(req.minQty());
        q.setMaxQty(req.maxQty());

        // 如果你希望「maxQty = 0」表示不限，也可以在這裡轉換規則
        // q.setMaxQty(req.maxQty());

        SellWindowQuota saved = repo.save(q);

        return new SellWindowQuotaUpsertDtos.Response(
                saved.getId(),
                saved.getSellWindowId(),
                saved.getProductId(),
                saved.getMinQty(),
                saved.getMaxQty(),
                saved.getSoldQty(),
                saved.getStatus(),
                saved.getUpdatedAt()
        );
    }

    private SellWindowQuotaBatchDto.QuotaResponse toDto(SellWindowQuota q) {
        return new SellWindowQuotaBatchDto.QuotaResponse(
                q.getSellWindowId(),
                q.getProductId(),
                q.getMinQty(),
                q.getMaxQty(),
                q.getSoldQty(),
                q.getStatus(),
                q.getUpdatedAt()
        );
    }
}
