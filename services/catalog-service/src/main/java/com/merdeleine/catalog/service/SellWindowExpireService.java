package com.merdeleine.catalog.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.catalog.client.OrderQuotaClient;
import com.merdeleine.catalog.entity.OutboxEvent;
import com.merdeleine.catalog.entity.SellWindow;
import com.merdeleine.catalog.enums.OutboxEventStatus;
import com.merdeleine.catalog.enums.SellWindowStatus;
import com.merdeleine.catalog.repository.OutboxEventRepository;
import com.merdeleine.catalog.repository.ProductSellWindowRepository;
import com.merdeleine.catalog.repository.SellWindowCandidate;
import com.merdeleine.catalog.repository.SellWindowRepository;
import com.merdeleine.messaging.SellWindowClosedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SellWindowExpireService {

    private final SellWindowRepository sellWindowRepository;
    private final ProductSellWindowRepository productSellWindowRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OrderQuotaClient orderQuotaClient;
    private final ObjectMapper objectMapper;
    private final String sellWindowClosedTopic;

    public SellWindowExpireService(
            SellWindowRepository sellWindowRepository,
            ProductSellWindowRepository productSellWindowRepository,
            OutboxEventRepository outboxEventRepository,
            OrderQuotaClient orderQuotaClient,
            ObjectMapper objectMapper,
            @Value("${merdeleine.kafka.topics.sell-window-closed}") String sellWindowClosedTopic
    ) {
        this.sellWindowRepository = sellWindowRepository;
        this.productSellWindowRepository = productSellWindowRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.orderQuotaClient = orderQuotaClient;
        this.objectMapper = objectMapper;
        this.sellWindowClosedTopic = sellWindowClosedTopic;
    }

    @Transactional
    public CloseExpiredResult closeExpired(int limit) {
        OffsetDateTime now = OffsetDateTime.now();

        List<SellWindowCandidate> candidates = sellWindowRepository.findExpiredOpenCandidates(
                now,
                SellWindowStatus.OPEN,
                PageRequest.of(0, Math.max(1, limit))
        );

        int casWonCount = 0;
        int totalClosedPsw = 0;
        int totalClosedQuotas = 0;
        int outboxInsertedCount = 0;
        List<Item> items = new ArrayList<>();

        for (SellWindowCandidate c : candidates) {
            UUID id = c.getId();

            // ✅ CAS 取得勝者
            int updated = sellWindowRepository.closeIfExpiredOpenAndVersionMatch(id, c.getVersion(), now);
            if (updated != 1) {
                // 沒搶到：可能已被關、或被延長、或狀態已變
                items.add(new Item(id, false, 0, 0, false));
                continue;
            }

            casWonCount++;

            // 重新讀 SellWindow 拿 name/endAt/timezone（也可改用 native returning，但先簡單穩）
            SellWindow sw = sellWindowRepository.findById(id)
                    .orElseThrow(() -> new IllegalStateException("SellWindow disappeared: " + id));

            CloseSellWindowResult closeResult = closeSellWindow(sw, now, "sell_window_expired");
            totalClosedPsw += closeResult.closedProductSellWindows();
            totalClosedQuotas += closeResult.closedSellWindowQuotas();
            if (closeResult.outboxInserted()) outboxInsertedCount++;

            items.add(new Item(id, true, closeResult.closedProductSellWindows(), closeResult.closedSellWindowQuotas(), closeResult.outboxInserted()));
        }

        return new CloseExpiredResult(
                now,
                candidates.size(),
                casWonCount,
                totalClosedPsw,
                totalClosedQuotas,
                outboxInsertedCount,
                items
        );
    }

    @Transactional
    public CloseSellWindowResult closeBySellWindowId(UUID sellWindowId) {
        OffsetDateTime now = OffsetDateTime.now();

        SellWindow sw = sellWindowRepository.findByIdForUpdate(sellWindowId)
                .orElseThrow(() -> new IllegalArgumentException("SellWindow not found: " + sellWindowId));

        ensureClosable(sw, now);
        return closeSellWindow(sw, now, "admin close sell_window");
    }

    private boolean insertSellWindowClosedOutboxOnce(SellWindow sw, OffsetDateTime closedAt) {
        String idempotencyKey = sellWindowClosedTopic + ":" + sw.getId();

        OutboxEvent e = new OutboxEvent();
        e.setId(UUID.randomUUID());
        e.setAggregateType("SellWindow");
        e.setAggregateId(sw.getId());
        e.setEventType(sellWindowClosedTopic);
        e.setIdempotencyKey(idempotencyKey);
        e.setPayload(buildSellWindowClosedPayload(sw, closedAt));
        e.setStatus(OutboxEventStatus.PENDING);

        try {
            outboxEventRepository.save(e);
            return true;
        } catch (DataIntegrityViolationException dup) {
            // ✅ idempotency_key unique 命中：代表事件早已寫過
            return false;
        }
    }

    private void ensureClosable(SellWindow sw, OffsetDateTime now) {
//        if (sw.getStatus() != SellWindowStatus.OPEN) {
//            throw new IllegalStateException("SellWindow is not OPEN: " + sw.getId() + ", status=" + sw.getStatus());
//        }
        if (sw.getEndAt() != null && sw.getEndAt().isAfter(now)) {
            throw new IllegalStateException("SellWindow is not expired yet: " + sw.getId());
        }
    }

    private CloseSellWindowResult closeSellWindow(SellWindow sw, OffsetDateTime now, String reason) {
        sw.setStatus(SellWindowStatus.CLOSED);
        sw.setClosedAt(now);

        int closedPsw = productSellWindowRepository.closeAllOpenBySellWindowId(sw.getId());

        int closedQuotas = 0;
        try {
            var quotaResp = orderQuotaClient.closeBySellWindow(sw.getId(), reason);
            if (quotaResp != null) {
                closedQuotas = quotaResp.closedCount();
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to close order quotas for sellWindowId=" + sw.getId(), ex);
        }

        boolean outboxInserted = insertSellWindowClosedOutboxOnce(sw, now);
        return new CloseSellWindowResult(sw.getId(), closedPsw, closedQuotas, outboxInserted);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize outbox payload", e);
        }
    }

    // --- DTOs ---

    public record SellWindowClosedV1(
            UUID sellWindowId,
            String sellWindowName,
            OffsetDateTime endAt,
            String timezone,
            OffsetDateTime closedAt
    ) {}

    public record CloseExpiredResult(
            OffsetDateTime now,
            int candidateCount,
            int casWonCount,
            int totalClosedProductSellWindows,
            int totalClosedSellWindowQuotas,
            int outboxInsertedCount,
            List<Item> items
    ) {}

    public record Item(
            UUID sellWindowId,
            boolean casWon,
            int closedProductSellWindows,
            int closedSellWindowQuotas,
            boolean outboxInserted
    ) {}

    public record CloseSellWindowResult(
            UUID sellWindowId,
            int closedProductSellWindows,
            int closedSellWindowQuotas,
            boolean outboxInserted
    ) {}

    // DTO -> JsonNode
    private JsonNode buildSellWindowClosedPayload(SellWindow sw, OffsetDateTime closedAt) {
        SellWindowClosedEvent payload = new SellWindowClosedEvent(
                UUID.randomUUID(),
                sellWindowClosedTopic,
                sw.getId(),
                null,
                closedAt
        );
        return objectMapper.valueToTree(payload);
    }
}
