package com.merdeleine.production.service;

import com.merdeleine.production.dto.BatchCounterDto;
import com.merdeleine.production.dto.CounterEventLogDto;
import com.merdeleine.production.entity.BatchCounter;
import com.merdeleine.production.entity.CounterEventLog;
import com.merdeleine.production.enums.BatchCounterStatus;
import com.merdeleine.production.exception.BadRequestException;
import com.merdeleine.production.exception.ConflictException;
import com.merdeleine.production.exception.NotFoundException;
import com.merdeleine.production.repository.BatchCounterRepository;
import com.merdeleine.production.repository.CounterEventLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BatchCounterService {

    private final BatchCounterRepository batchCounterRepository;
    private final CounterEventLogRepository counterEventLogRepository;

    public BatchCounterService(BatchCounterRepository batchCounterRepository,
                               CounterEventLogRepository counterEventLogRepository) {
        this.batchCounterRepository = batchCounterRepository;
        this.counterEventLogRepository = counterEventLogRepository;
    }

    @Transactional
    public BatchCounterDto.Response create(BatchCounterDto.CreateRequest req) {
        if (req.getSellWindowId() == null) throw new BadRequestException("sellWindowId is required");
        if (req.getProductId() == null) throw new BadRequestException("productId is required");
        if (req.getThresholdQty() == null || req.getThresholdQty() <= 0) throw new BadRequestException("thresholdQty must be >= 1");
        if (req.getStatus() == null) throw new BadRequestException("status is required");

        if (batchCounterRepository.existsBySellWindowIdAndProductId(req.getSellWindowId(), req.getProductId())) {
            throw new ConflictException("BatchCounter already exists for sellWindowId + productId");
        }

        BatchCounter e = new BatchCounter();
        e.setId(UUID.randomUUID());
        e.setSellWindowId(req.getSellWindowId());
        e.setProductId(req.getProductId());
        e.setPaidQty(0);
        e.setThresholdQty(req.getThresholdQty());
        e.setStatus(req.getStatus());

        return toResponse(batchCounterRepository.save(e));
    }

    @Transactional(readOnly = true)
    public BatchCounterDto.Response get(UUID id) {
        BatchCounter e = batchCounterRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("BatchCounter not found: " + id));
        return toResponse(e);
    }

    @Transactional(readOnly = true)
    public List<BatchCounterDto.Response> list(UUID sellWindowId, UUID productId) {
        if (sellWindowId != null && productId != null) {
            return batchCounterRepository.findBySellWindowIdAndProductId(sellWindowId, productId)
                    .map(e -> List.of(toResponse(e)))
                    .orElse(List.of());
        }
        return batchCounterRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public BatchCounterDto.Response update(UUID id, BatchCounterDto.UpdateRequest req) {
        if (req.getThresholdQty() == null || req.getThresholdQty() <= 0) throw new BadRequestException("thresholdQty must be >= 1");
        if (req.getStatus() == null) throw new BadRequestException("status is required");

        BatchCounter e = batchCounterRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("BatchCounter not found: " + id));

        e.setThresholdQty(req.getThresholdQty());
        e.setStatus(req.getStatus());

        // 如果你允許 update threshold/status 影響 reached 狀態，可在此加規則
        return toResponse(batchCounterRepository.save(e));
    }

    @Transactional
    public void delete(UUID id) {
        if (!batchCounterRepository.existsById(id)) {
            throw new NotFoundException("BatchCounter not found: " + id);
        }
        batchCounterRepository.deleteById(id);
    }

    /**
     * 核心動作：收到 payment/order paid 事件後，把 paidQty += deltaQty，並寫入 CounterEventLog。
     * - 以 sourceEventId 做去重（同事件只處理一次）
     * - 到量時標記 reachedAt / reachedEventId / status
     */
    @Transactional
    public BatchCounterDto.Response applyPaidEvent(UUID counterId, BatchCounterDto.ApplyPaidEventRequest req) {
        if (req.getSourceEventType() == null || req.getSourceEventType().isBlank()) {
            throw new BadRequestException("sourceEventType is required");
        }
        if (req.getSourceEventId() == null) throw new BadRequestException("sourceEventId is required");
        if (req.getDeltaQty() == null || req.getDeltaQty() <= 0) throw new BadRequestException("deltaQty must be >= 1");

        BatchCounter counter = batchCounterRepository.findById(counterId)
                .orElseThrow(() -> new NotFoundException("BatchCounter not found: " + counterId));

        // 去重：同 sourceEventId 不可重複寫入
        if (counterEventLogRepository.existsBySourceEventId(req.getSourceEventId())) {
            throw new ConflictException("Duplicate sourceEventId: " + req.getSourceEventId());
        }

        // 更新 qty
        int currentPaid = counter.getPaidQty() == null ? 0 : counter.getPaidQty();
        int newPaid = currentPaid + req.getDeltaQty();
        counter.setPaidQty(newPaid);

        // 寫入 log（用 counter.addEventLog 維持雙向關聯）
        CounterEventLog log = new CounterEventLog();
        log.setId(UUID.randomUUID());
        log.setSourceEventType(req.getSourceEventType());
        log.setSourceEventId(req.getSourceEventId());
        log.setDeltaQty(req.getDeltaQty());
        counter.addEventLog(log);

        // 到量判斷：只有第一次到量才填 reachedAt / reachedEventId
        if (newPaid >= counter.getThresholdQty()) {
            if (counter.getReachedAt() == null) {
                counter.setReachedAt(OffsetDateTime.now());
                counter.setReachedEventId(req.getSourceEventId());
            }
            // 狀態你要怎麼定：這裡示意改成 REACHED（請用你 enum 的實際值）
            // 如果你的 enum 沒有 REACHED，請改成你定義的狀態（例如 THRESHOLD_REACHED）
            counter.setStatus(guessReachedStatus(counter.getStatus()));
        }

        BatchCounter saved = batchCounterRepository.save(counter);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CounterEventLogDto.Response> listEventLogs(UUID counterId) {
        // 如果 counter 不存在，直接 404（避免回空列表掩蓋問題）
        if (!batchCounterRepository.existsById(counterId)) {
            throw new NotFoundException("BatchCounter not found: " + counterId);
        }
        return counterEventLogRepository.findByCounter_IdOrderByCreatedAtAsc(counterId)
                .stream()
                .map(this::toLogResponse)
                .toList();
    }

    private BatchCounterDto.Response toResponse(BatchCounter e) {
        return new BatchCounterDto.Response(
                e.getId(),
                e.getSellWindowId(),
                e.getProductId(),
                e.getPaidQty(),
                e.getThresholdQty(),
                e.getStatus(),
                e.getReachedAt(),
                e.getReachedEventId(),
                e.getUpdatedAt()
        );
    }

    private CounterEventLogDto.Response toLogResponse(CounterEventLog e) {
        UUID counterId = e.getCounter() == null ? null : e.getCounter().getId();
        return new CounterEventLogDto.Response(
                e.getId(),
                counterId,
                e.getSourceEventType(),
                e.getSourceEventId(),
                e.getDeltaQty(),
                e.getCreatedAt()
        );
    }

    /**
     * 這個方法是為了避免我不知道你 BatchCounterStatus 的實際 enum 值。
     * 你可以把它改成：
     *   return BatchCounterStatus.THRESHOLD_REACHED;
     */
    private BatchCounterStatus guessReachedStatus(BatchCounterStatus current) {
        // TODO: 請依你的 enum 實際值調整
        return current;
    }
}
