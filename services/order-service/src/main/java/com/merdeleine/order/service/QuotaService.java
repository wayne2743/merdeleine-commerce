package com.merdeleine.order.service;


import com.merdeleine.order.exception.SoldOutException;
import com.merdeleine.order.repository.SellWindowQuotaRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class QuotaService {

    private final SellWindowQuotaRepository quotaRepo;

    public QuotaService(SellWindowQuotaRepository quotaRepo) {
        this.quotaRepo = quotaRepo;
    }

    @Transactional
    public void reserveOrThrow(UUID sellWindowId, UUID productId, UUID variantId, int qty) {
        int updated = quotaRepo.tryReserve(sellWindowId, productId, variantId, qty);
        if (updated == 0) {
            throw new SoldOutException("Sold out or quota closed");
        }
    }

    @Transactional
    public void release(UUID sellWindowId, UUID productId, UUID variantId, int qty) {
        quotaRepo.release(sellWindowId, productId, variantId, qty);
    }
}
