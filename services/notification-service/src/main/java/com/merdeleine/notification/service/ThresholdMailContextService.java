package com.merdeleine.notification.service;

import com.merdeleine.notification.client.CatalogServiceClient;
import com.merdeleine.notification.dto.RefsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ThresholdMailContextService {

    private static final Logger log = LoggerFactory.getLogger(ThresholdMailContextService.class);
    private final CatalogServiceClient catalogServiceClient;

    public ThresholdMailContextService(CatalogServiceClient catalogServiceClient) {

        this.catalogServiceClient = catalogServiceClient;
    }

    public MailContext build(UUID productId, UUID sellWindowId) {
        try {
            log.info("Fetching refs for productId={}, sellWindowId={}", productId, sellWindowId);
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
