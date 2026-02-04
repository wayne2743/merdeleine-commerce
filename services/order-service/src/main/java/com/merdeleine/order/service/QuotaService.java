package com.merdeleine.order.service;


import com.merdeleine.messaging.SellWindowQuotaConfiguredEvent;
import com.merdeleine.order.exception.SoldOutException;
import com.merdeleine.order.repository.SellWindowQuotaRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class QuotaService {

    private final SellWindowQuotaRepository quotaRepository;

    public QuotaService(SellWindowQuotaRepository quotaRepository) {
        this.quotaRepository = quotaRepository;
    }

    @Transactional
    public void reserveOrThrow(UUID sellWindowId, UUID productId, int qty) {
        int updated = quotaRepository.tryReserve(sellWindowId, productId, qty);
        if (updated == 0) {
            throw new SoldOutException("Sold out or quota closed");
        }
    }

    @Transactional
    public void release(UUID sellWindowId, UUID productId, int qty) {
        quotaRepository.release(sellWindowId, productId, qty);
    }

    @Transactional
    public void apply(SellWindowQuotaConfiguredEvent event) {

        // UPSERT：存在就更新，不存在就建立
        quotaRepository.upsert(
                UUID.randomUUID(),
                event.sellWindowId(),
                event.productId(),
                event.minQty(),
                event.maxQty()
        );
    }

    public boolean isProcessed(UUID sellWindowId, UUID productId) {
        return quotaRepository.existsBySellWindowIdAndProductId(sellWindowId, productId);
    }
}
