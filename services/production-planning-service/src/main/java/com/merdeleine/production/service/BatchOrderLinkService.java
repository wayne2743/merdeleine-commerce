package com.merdeleine.production.service;


import com.merdeleine.production.dto.BatchOrderLinkCreateRequest;
import com.merdeleine.production.dto.BatchOrderLinkUpdateRequest;
import com.merdeleine.production.entity.Batch;
import com.merdeleine.production.entity.BatchOrderLink;
import com.merdeleine.production.repository.BatchOrderLinkRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BatchOrderLinkService {

    private final BatchService batchService;
    private final BatchOrderLinkRepository repo;

    public BatchOrderLinkService(BatchService batchService, BatchOrderLinkRepository repo) {
        this.batchService = batchService;
        this.repo = repo;
    }

    @Transactional
    public BatchOrderLink create(UUID batchId, BatchOrderLinkCreateRequest req) {
        Batch batch = batchService.get(batchId);

        BatchOrderLink link = new BatchOrderLink();
        link.setId(UUID.randomUUID());
        link.setBatch(batch);
        link.setOrderId(req.getOrderId());
        link.setQuantity(req.getQuantity());

        // 也可以用 batch.addOrderLink(link) 再 save(batch)；這裡直接存 link 最直觀
        return repo.save(link);
    }

    @Transactional(readOnly = true)
    public List<BatchOrderLink> list(UUID batchId) {
        // 確認 batch 存在
        batchService.get(batchId);
        return repo.findByBatch_Id(batchId);
    }

    @Transactional(readOnly = true)
    public BatchOrderLink get(UUID batchId, UUID linkId) {
        return repo.findByIdAndBatch_Id(linkId, batchId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "BatchOrderLink not found: batchId=" + batchId + ", linkId=" + linkId));
    }

    @Transactional
    public BatchOrderLink update(UUID batchId, UUID linkId, BatchOrderLinkUpdateRequest req) {
        BatchOrderLink link = get(batchId, linkId);
        if (req.getQuantity() != null) link.setQuantity(req.getQuantity());
        return repo.save(link);
    }

    @Transactional
    public void delete(UUID batchId, UUID linkId) {
        if (!repo.existsByIdAndBatch_Id(linkId, batchId)) {
            throw new EntityNotFoundException(
                    "BatchOrderLink not found: batchId=" + batchId + ", linkId=" + linkId);
        }
        repo.deleteByIdAndBatch_Id(linkId, batchId);
    }
}
