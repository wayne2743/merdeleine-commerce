package com.merdeleine.notification.service;

import com.merdeleine.notification.client.CatalogServiceClient;
import com.merdeleine.notification.dto.RefsResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ThresholdMailContextService {

    private final CatalogServiceClient catalogServiceClient;

    public ThresholdMailContextService(CatalogServiceClient catalogServiceClient) {

        this.catalogServiceClient = catalogServiceClient;
    }

    public MailContext build(UUID productId, UUID sellWindowId) {
        try {
            RefsResponse refs = catalogServiceClient.getRefs(productId, sellWindowId);
            return new MailContext(
                    productId,
                    sellWindowId,
                    refs.productName(),
                    refs.sellWindowName()
            );
        } catch (Exception ex) {
            // ✅ 重點：通知服務不要因為查名字失敗就整個事件處理失敗
            // 你可以在這邊打 warn log
            return new MailContext(
                    productId,
                    sellWindowId,
                    "(unknown product)",
                    "(unknown sell window)"
            );
        }
    }

    public record MailContext(
            UUID productId,
            UUID sellWindowId,
            String productName,
            String sellWindowName
    ) {}
}
