package com.merdeleine.production.planning.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.messaging.BatchConfirmEvent;
import com.merdeleine.production.planning.client.OrderServiceClient;
import com.merdeleine.production.planning.dto.BatchCreateRequest;
import com.merdeleine.production.planning.dto.BatchUpdateRequest;
import com.merdeleine.production.planning.entity.Batch;
import com.merdeleine.production.planning.entity.OutboxEvent;
import com.merdeleine.production.planning.enums.OutboxEventStatus;
import com.merdeleine.production.planning.repository.BatchRepository;
import com.merdeleine.production.planning.repository.OutboxEventRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BatchService {

    private final BatchRepository batchRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OrderServiceClient orderServiceClient;
    private final String batchConfirmedTopic;
    private final ObjectMapper objectMapper;

    public BatchService(BatchRepository batchRepository, OutboxEventRepository outboxEventRepository,
                        OrderServiceClient orderServiceClient,
                        @Value("${app.kafka.topic.batch-confirm-events}") String batchConfirmedTopic,
                        ObjectMapper objectMapper) {
        this.batchRepository = batchRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.orderServiceClient = orderServiceClient;
        this.batchConfirmedTopic = batchConfirmedTopic;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Batch create(BatchCreateRequest req) {
        Batch b = new Batch();
        b.setId(UUID.randomUUID());
        b.setSellWindowId(req.getSellWindowId());
        b.setProductId(req.getProductId());
        b.setTargetQty(req.getTargetQty());
        b.setStatus(req.getStatus());
        b.setConfirmedAt(req.getConfirmedAt());
        return batchRepository.save(b);
    }

    @Transactional(readOnly = true)
    public Batch get(UUID id) {
        return batchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Batch not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Batch> list(Pageable pageable) {
        return batchRepository.findAll(pageable);
    }

    @Transactional
    public Batch update(UUID id, BatchUpdateRequest req) {
        Batch b = get(id);

        if (req.getTargetQty() != null) b.setTargetQty(req.getTargetQty());
        if (req.getStatus() != null) b.setStatus(req.getStatus());
        if (req.getConfirmedAt() != null) b.setConfirmedAt(req.getConfirmedAt());

        return batchRepository.save(b);
    }

    @Transactional
    public void delete(UUID id) {
        if (!batchRepository.existsById(id)) {
            throw new EntityNotFoundException("Batch not found: " + id);
        }
        batchRepository.deleteById(id);
    }

    public Batch confirm(UUID batchId) {

        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("Batch not found: " + batchId));
        // 1) 先關單（強一致 gate）
        OrderServiceClient.CloseQuotaResponse resp = orderServiceClient.closeQuota(
                batch.getSellWindowId(),
                batch.getProductId(),
                batchId,
                "LOCK_QUOTA_FOR_PAYMENT"
        );

        writeOutbox(
                "Batch",
                batchId,
                batchConfirmedTopic,
                new BatchConfirmEvent(
                        UUID.randomUUID(),
                        batchConfirmedTopic,
                        batchId,
                        batch.getProductId(),
                        batch.getSellWindowId()
                )
        );
        return batch;
    }

    private void writeOutbox(String aggregateType, UUID aggregateId, String eventType, Object payloadObj) {
        try {
            OutboxEvent evt = new OutboxEvent();
            evt.setId(UUID.randomUUID());
            evt.setAggregateType(aggregateType);
            evt.setAggregateId(aggregateId);
            evt.setEventType(eventType);
            evt.setPayload(objectMapper.valueToTree(payloadObj));
            evt.setStatus(OutboxEventStatus.NEW);
            outboxEventRepository.save(evt);
        } catch (Exception e) {
            // 讓 transaction rollback，確保「業務寫入 + outbox」同生共死
            throw new RuntimeException("Failed to write outbox event", e);
        }
    }

}
