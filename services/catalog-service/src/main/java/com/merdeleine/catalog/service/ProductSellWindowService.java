package com.merdeleine.catalog.service;

import com.merdeleine.catalog.domain.ProductSellWindow;
import com.merdeleine.catalog.domain.SellWindow;
import com.merdeleine.catalog.dto.ProductSellWindowDto;
import com.merdeleine.catalog.entity.Product;
import com.merdeleine.catalog.exception.BadRequestException;
import com.merdeleine.catalog.exception.NotFoundException;
import com.merdeleine.catalog.repository.ProductSellWindowRepository;
import com.merdeleine.catalog.repository.SellWindowRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductSellWindowService {

    private final ProductSellWindowRepository pswRepository;
    private final SellWindowRepository sellWindowRepository;
    private final EntityManager entityManager;

    public ProductSellWindowService(ProductSellWindowRepository pswRepository,
                                    SellWindowRepository sellWindowRepository,
                                    EntityManager entityManager) {
        this.pswRepository = pswRepository;
        this.sellWindowRepository = sellWindowRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public ProductSellWindowDto.Response create(ProductSellWindowDto.CreateRequest req) {
        if (req.getProductId() == null) throw new BadRequestException("productId is required");
        if (req.getSellWindowId() == null) throw new BadRequestException("sellWindowId is required");
        if (req.getLeadDays() != null && req.getLeadDays() < 0) throw new BadRequestException("leadDays must be >= 0");
        if (req.getShipDays() != null && req.getShipDays() < 0) throw new BadRequestException("shipDays must be >= 0");
        if (req.getMaxTotalQty() != null && req.getMaxTotalQty() < req.getThresholdQty()) {
            throw new BadRequestException("maxTotalQty must be >= thresholdQty");
        }

        // 防重：同一個 product + sellWindow 只能一筆
        Optional<ProductSellWindow> existing =
                pswRepository.findByProduct_IdAndSellWindow_Id(req.getProductId(), req.getSellWindowId());
        if (existing.isPresent()) {
            throw new BadRequestException("ProductSellWindow already exists for productId + sellWindowId");
        }

        // 關聯存在性檢查：SellWindow 必須存在
        SellWindow sellWindow = sellWindowRepository.findById(req.getSellWindowId())
                .orElseThrow(() -> new NotFoundException("SellWindow not found: " + req.getSellWindowId()));

        // 關聯存在性檢查：Product 必須存在
        // 這裡用 getReference (lazy proxy)，但先 exists 檢查避免 FK 爆炸
        Product product = entityManager.getReference(Product.class, req.getProductId());

        ProductSellWindow e = new ProductSellWindow();
        e.setProduct(product);
        e.setSellWindow(sellWindow);
        e.setThresholdQty(req.getThresholdQty());
        e.setMaxTotalQty(req.getMaxTotalQty());
        e.setLeadDays(req.getLeadDays());
        e.setShipDays(req.getShipDays());
        if (req.getEnabled() != null) {
            e.setEnabled(req.getEnabled());
        }

        return toResponse(pswRepository.save(e));
    }

    @Transactional(readOnly = true)
    public ProductSellWindowDto.Response get(UUID id) {
        ProductSellWindow e = pswRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ProductSellWindow not found: " + id));
        return toResponse(e);
    }

    @Transactional(readOnly = true)
    public List<ProductSellWindowDto.Response> list(UUID productId, UUID sellWindowId) {
        if (productId != null && sellWindowId != null) {
            return pswRepository.findByProduct_IdAndSellWindow_Id(productId, sellWindowId)
                    .map(e -> List.of(toResponse(e)))
                    .orElse(List.of());
        }
        if (productId != null) {
            return pswRepository.findByProduct_Id(productId).stream().map(this::toResponse).toList();
        }
        if (sellWindowId != null) {
            return pswRepository.findBySellWindow_Id(sellWindowId).stream().map(this::toResponse).toList();
        }
        return pswRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ProductSellWindowDto.Response update(UUID id, ProductSellWindowDto.UpdateRequest req) {
        if (req.getThresholdQty() <= 0) throw new BadRequestException("thresholdQty must be >= 1");
        if (req.getLeadDays() != null && req.getLeadDays() < 0) throw new BadRequestException("leadDays must be >= 0");
        if (req.getShipDays() != null && req.getShipDays() < 0) throw new BadRequestException("shipDays must be >= 0");
        if (req.getMaxTotalQty() != null && req.getMaxTotalQty() < req.getThresholdQty()) {
            throw new BadRequestException("maxTotalQty must be >= thresholdQty");
        }

        ProductSellWindow e = pswRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ProductSellWindow not found: " + id));

        e.setThresholdQty(req.getThresholdQty());
        e.setMaxTotalQty(req.getMaxTotalQty());
        e.setLeadDays(req.getLeadDays());
        e.setShipDays(req.getShipDays());
        if (req.getEnabled() != null) {
            e.setEnabled(req.getEnabled());
        }

        return toResponse(pswRepository.save(e));
    }

    @Transactional
    public void delete(UUID id) {
        if (!pswRepository.existsById(id)) {
            throw new NotFoundException("ProductSellWindow not found: " + id);
        }
        pswRepository.deleteById(id);
    }

    private ProductSellWindowDto.Response toResponse(ProductSellWindow e) {
        return new ProductSellWindowDto.Response(
                e.getId(),
                e.getProduct().getId(),
                e.getSellWindow().getId(),
                e.getThresholdQty(),
                e.getMaxTotalQty(),
                e.getLeadDays(),
                e.getShipDays(),
                e.isEnabled()
        );
    }
}
