package com.merdeleine.catalog.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.merdeleine.catalog.entity.OutboxEvent;
import com.merdeleine.catalog.entity.SellWindow;
import com.merdeleine.catalog.enums.OutboxEventStatus;
import com.merdeleine.catalog.enums.SellWindowStatus;
import com.merdeleine.catalog.repository.OutboxEventRepository;
import com.merdeleine.catalog.repository.ProductSellWindowRepository;
import com.merdeleine.catalog.repository.SellWindowCandidate;
import com.merdeleine.catalog.repository.SellWindowRepository;
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
    private final ObjectMapper objectMapper;
    private final String sellWindowClosedTopic;

    public SellWindowExpireService(
            SellWindowRepository sellWindowRepository,
            ProductSellWindowRepository productSellWindowRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            @Value("${app.outbox.event-types.sell-window-closed:sellwindow.closed.v1}") String sellWindowClosedTopic
    ) {
        this.sellWindowRepository = sellWindowRepository;
        this.productSellWindowRepository = productSellWindowRepository;
        this.outboxEventRepository = outboxEventRepository;
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
        int outboxInsertedCount = 0;
        List<Item> items = new ArrayList<>();

        for (SellWindowCandidate c : candidates) {
            UUID id = c.getId();

            // ✅ CAS 取得勝者
            int updated = sellWindowRepository.closeIfExpiredOpenAndVersionMatch(id, c.getVersion(), now);
            if (updated != 1) {
                // 沒搶到：可能已被關、或被延長、或狀態已變
                items.add(new Item(id, false, 0, false));
                continue;
            }

            casWonCount++;

            // 勝者才關閉 ProductSellWindow
            int closedPsw = productSellWindowRepository.closeAllOpenBySellWindowId(id);
            totalClosedPsw += closedPsw;

            // 重新讀 SellWindow 拿 name/endAt/timezone（也可改用 native returning，但先簡單穩）
            SellWindow sw = sellWindowRepository.findById(id)
                    .orElseThrow(() -> new IllegalStateException("SellWindow disappeared: " + id));

            boolean outboxInserted = insertSellWindowClosedOutboxOnce(sw, now);
            if (outboxInserted) outboxInsertedCount++;

            items.add(new Item(id, true, closedPsw, outboxInserted));
        }

        return new CloseExpiredResult(
                now,
                candidates.size(),
                casWonCount,
                totalClosedPsw,
                outboxInsertedCount,
                items
        );
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
            int outboxInsertedCount,
            List<Item> items
    ) {}

    public record Item(
            UUID sellWindowId,
            boolean casWon,
            int closedProductSellWindows,
            boolean outboxInserted
    ) {}

    // DTO -> JsonNode
    private JsonNode buildSellWindowClosedPayload(SellWindow sw, OffsetDateTime closedAt) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("sellWindowId", sw.getId().toString());
        node.put("sellWindowName", sw.getName());
        node.put("endAt", sw.getEndAt().toString());
        node.put("timezone", sw.getTimezone());
        node.put("closedAt", closedAt.toString());
        return node;
    }
}
