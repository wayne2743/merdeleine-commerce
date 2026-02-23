package com.merdeleine.order.service;


import com.merdeleine.order.dto.CloseQuotaDtos;
import com.merdeleine.order.dto.SellWindowQuotaBatchDto;
import com.merdeleine.order.entity.SellWindowQuota;
import com.merdeleine.order.repository.SellWindowQuotaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SellWindowQuotaService {

    public static final String STATUS_CLOSED = "CLOSED";

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
