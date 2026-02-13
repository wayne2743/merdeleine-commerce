package com.merdeleine.catalog.mapper;

import com.merdeleine.catalog.entity.ProductSellWindow;
import com.merdeleine.messaging.SellWindowQuotaConfiguredEvent;

import java.time.OffsetDateTime;
import java.util.UUID;

public class SellWindowQuotaEventMapper {
    public  SellWindowQuotaConfiguredEvent toSellWindowQuotaConfiguredEvent(ProductSellWindow productSellWindow, String eventType) {
        return new SellWindowQuotaConfiguredEvent(
                UUID.randomUUID(),
                eventType,
                productSellWindow.getSellWindow().getId(),
                productSellWindow.getProduct().getId(),
                productSellWindow.getMinTotalQty(),
                productSellWindow.getMaxTotalQty(),
                OffsetDateTime.now()
        );
    }
}
