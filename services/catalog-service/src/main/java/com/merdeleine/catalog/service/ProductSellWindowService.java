package com.merdeleine.catalog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.catalog.client.ThresholdServiceClient;
import com.merdeleine.catalog.dto.PageResponse;
import com.merdeleine.catalog.dto.ProductOpenSellWindowResponse;
import com.merdeleine.catalog.dto.ProductSellWindowDto;
import com.merdeleine.catalog.dto.SellWindowDto;
import com.merdeleine.catalog.dto.threshold.BatchCounterLookupRequest;
import com.merdeleine.catalog.dto.threshold.BatchCounterResponse;
import com.merdeleine.catalog.dto.threshold.BatchCounterRequest;
import com.merdeleine.catalog.entity.OutboxEvent;
import com.merdeleine.catalog.entity.Product;
import com.merdeleine.catalog.entity.ProductSellWindow;
import com.merdeleine.catalog.entity.SellWindow;
import com.merdeleine.catalog.enums.OutboxEventStatus;
import com.merdeleine.catalog.enums.SellWindowStatus;
import com.merdeleine.catalog.exception.BadRequestException;
import com.merdeleine.catalog.exception.NotFoundException;
import com.merdeleine.catalog.mapper.SellWindowQuotaEventMapper;
import com.merdeleine.catalog.repository.OutboxEventRepository;
import com.merdeleine.catalog.repository.ProductRepository;
import com.merdeleine.catalog.repository.ProductSellWindowRepository;
import com.merdeleine.catalog.repository.SellWindowRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductSellWindowService {

    private final ProductSellWindowRepository pswRepository;
    private final ProductRepository productRepository;
    private final SellWindowRepository sellWindowRepository;
    private final ThresholdServiceClient thresholdClient;
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;
    private final String sellWindowQuotaConfiguredTopic;

    public ProductSellWindowService(ProductSellWindowRepository pswRepository,
                                    ProductRepository productRepository,
                                    SellWindowRepository sellWindowRepository,
                                    ThresholdServiceClient thresholdClient,
                                    ObjectMapper objectMapper,
                                    OutboxEventRepository outboxEventRepository,
                                    @Value("${merdeleine.kafka.topics.sell-window-quota-configured}") String sellWindowQuotaConfiguredTopic) {
        this.pswRepository = pswRepository;
        this.productRepository = productRepository;
        this.sellWindowRepository = sellWindowRepository;
        this.thresholdClient = thresholdClient;
        this.objectMapper = objectMapper;
        this.outboxEventRepository = outboxEventRepository;
        this.sellWindowQuotaConfiguredTopic = sellWindowQuotaConfiguredTopic;
    }

    @Transactional
    public ProductSellWindowDto.Response create(ProductSellWindowDto.CreateRequest req) throws JsonProcessingException {
        if (req.getProductId() == null) throw new BadRequestException("productId is required");
        if (req.getSellWindowId() == null) throw new BadRequestException("sellWindowId is required");
        if (req.getLeadDays() != null && req.getLeadDays() < 0) throw new BadRequestException("leadDays must be >= 0");
        if (req.getShipDays() != null && req.getShipDays() < 0) throw new BadRequestException("shipDays must be >= 0");
        if (req.getMaxTotalQty() != null && req.getMaxTotalQty() < req.getMinTotalQty()) {
            throw new BadRequestException("maxTotalQty must be >= minTotalQty");
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

        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found: " + req.getProductId()));

        ProductSellWindow psw = new ProductSellWindow();
        psw.setProduct(product);
        psw.setSellWindow(sellWindow);
        psw.setMinTotalQty(req.getMinTotalQty());
        psw.setMaxTotalQty(req.getMaxTotalQty());
        psw.setLeadDays(req.getLeadDays());
        psw.setShipDays(req.getShipDays());
        psw.setUnitPriceCents(product.getUnitPriceCents());
        psw.setCurrency(product.getCurrency() != null ? product.getCurrency() : "TWD");
        if (req.isClosed() != null) {
            psw.setClosed(req.isClosed());
        }

        ProductSellWindow saved = pswRepository.save(psw);
                ;

        writeOutbox(
                "ProductSellWindow",
                saved.getId(),
                sellWindowQuotaConfiguredTopic,
                new SellWindowQuotaEventMapper().toSellWindowQuotaConfiguredEvent(saved, sellWindowQuotaConfiguredTopic)
        );

        onProductSellWindowCreated(saved.getSellWindow().getId(), saved.getProduct().getId(), saved.getMinTotalQty());

        return toResponse(saved);
    }

    @Transactional
    public ProductSellWindowDto.Response createWithSellWindow(ProductSellWindowDto.CreateWithSellWindowRequest req)
            throws JsonProcessingException {
        if (req.getProductId() == null) throw new BadRequestException("productId is required");
        if (req.getSellWindow() == null) throw new BadRequestException("sellWindow is required");

        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found: " + req.getProductId()));

        validateSellWindow(
                req.getSellWindow().getName(),
                req.getSellWindow().getStartAt(),
                req.getSellWindow().getEndAt(),
                req.getSellWindow().getTimezone()
        );

        if (sellWindowRepository.existsByName(req.getSellWindow().getName())) {
            throw new BadRequestException("SellWindow name already exists: " + req.getSellWindow().getName());
        }

        int minTotalQty = req.getMinTotalQty() != null
                ? req.getMinTotalQty()
                : (product.getDefaultMinQty() != null ? product.getDefaultMinQty() : 1);
        Integer maxTotalQty = req.getMaxTotalQty() != null ? req.getMaxTotalQty() : product.getDefaultMaxQty();
        Integer leadDays = req.getLeadDays() != null ? req.getLeadDays() : product.getDefaultLeadDays();
        Integer shipDays = req.getShipDays() != null ? req.getShipDays() : product.getDefaultShipDays();

        if (minTotalQty <= 0) throw new BadRequestException("minTotalQty must be > 0");
        if (leadDays != null && leadDays < 0) throw new BadRequestException("leadDays must be >= 0");
        if (shipDays != null && shipDays < 0) throw new BadRequestException("shipDays must be >= 0");
        if (maxTotalQty != null && maxTotalQty < minTotalQty) {
            throw new BadRequestException("maxTotalQty must be >= minTotalQty");
        }

        SellWindow sellWindow = new SellWindow();
        sellWindow.setName(req.getSellWindow().getName());
        sellWindow.setStartAt(req.getSellWindow().getStartAt());
        sellWindow.setEndAt(req.getSellWindow().getEndAt());
        sellWindow.setTimezone(req.getSellWindow().getTimezone());
        sellWindow.setPredictedPaymentDate(req.getSellWindow().getPredictedPaymentDate());
        sellWindow.setPredictedProdDate(req.getSellWindow().getPredictedProdDate());
        sellWindow.setPredictedShipDate(req.getSellWindow().getPredictedShipDate());
        sellWindow.setStatus(SellWindowStatus.OPEN);
        SellWindow savedSellWindow = sellWindowRepository.save(sellWindow);

        ProductSellWindow psw = new ProductSellWindow();
        psw.setProduct(product);
        psw.setSellWindow(savedSellWindow);
        psw.setMinTotalQty(minTotalQty);
        psw.setMaxTotalQty(maxTotalQty);
        psw.setLeadDays(leadDays);
        psw.setShipDays(shipDays);
        psw.setUnitPriceCents(product.getUnitPriceCents());
        psw.setCurrency(product.getCurrency() != null ? product.getCurrency() : "TWD");
        if (req.getIsClosed() != null) {
            psw.setClosed(req.getIsClosed());
        }

        ProductSellWindow saved = pswRepository.save(psw);

        writeOutbox(
                "ProductSellWindow",
                saved.getId(),
                sellWindowQuotaConfiguredTopic,
                new SellWindowQuotaEventMapper().toSellWindowQuotaConfiguredEvent(saved, sellWindowQuotaConfiguredTopic)
        );

        onProductSellWindowCreated(savedSellWindow.getId(), product.getId(), minTotalQty);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductSellWindowDto.Response get(UUID id) {
        ProductSellWindow e = pswRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ProductSellWindow not found: " + id));
        return toResponse(e);
    }

    @Transactional(readOnly = true)
    public ProductSellWindowDto.CombinedResponse getCombined(UUID id) {
        ProductSellWindow e = pswRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ProductSellWindow not found: " + id));
        return toCombinedResponse(e, loadCounterMap(List.of(e)).get(key(e.getSellWindow().getId(), e.getProduct().getId())));
    }

    @Transactional(readOnly = true)
    public List<ProductSellWindowDto.CombinedResponse> listCombined(UUID productId, UUID sellWindowId) {
        var rows = pswRepository.findAllWithRefs(productId, sellWindowId);
        var countersByKey = loadCounterMap(rows);

        return rows.stream()
                .map(e -> toCombinedResponse(e, countersByKey.get(key(e.getSellWindow().getId(), e.getProduct().getId()))))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSellWindowDto.CombinedResponse> pageCombined(UUID productId, UUID sellWindowId, Pageable pageable) {
        int safePage = Math.max(pageable.getPageNumber(), 0);
        int safeSize = Math.min(Math.max(pageable.getPageSize(), 1), 100);
        PageRequest safePageRequest = PageRequest.of(safePage, safeSize, pageable.getSort());

        var rowsPage = pswRepository.pageWithRefs(productId, sellWindowId, safePageRequest);
        var countersByKey = loadCounterMap(rowsPage.getContent());
        var items = rowsPage.getContent().stream()
                .map(row -> toCombinedResponseFromRow(row, countersByKey.get(key(row.getSellWindowId(), row.getProductId()))))
                .toList();

        return new PageResponse<>(items, safePage, safeSize, rowsPage.getTotalElements());
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

    @Transactional(readOnly = true)
    public ProductOpenSellWindowResponse findOpenByProductId(UUID productId) {
        List<UUID> ids = pswRepository.findOpenProductSellWindowIdsByProductId(productId, PageRequest.of(0, 1));
        UUID productSellWindowId = ids.isEmpty() ? null : ids.get(0);
        return new ProductOpenSellWindowResponse(productSellWindowId);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSellWindowDto.PageItem> page(UUID productId, UUID sellWindowId, Pageable pageable) {
        int safePage = Math.max(pageable.getPageNumber(), 0);
        int safeSize = Math.min(Math.max(pageable.getPageSize(), 1), 100);
        PageRequest safePageRequest = PageRequest.of(safePage, safeSize, pageable.getSort());

        var rowsPage = pswRepository.pageWithRefs(productId, sellWindowId, safePageRequest);
        var items = rowsPage.getContent().stream()
                .map(this::toPageItem)
                .toList();

        return new PageResponse<>(items, safePage, safeSize, rowsPage.getTotalElements());
    }

    @Transactional
    public ProductSellWindowDto.Response update(UUID id, ProductSellWindowDto.UpdateRequest req) {
        if (req.getMinTotalQty() <= 0) throw new BadRequestException("minTotalQty must be > 0");
        if (req.getLeadDays() != null && req.getLeadDays() < 0) throw new BadRequestException("leadDays must be >= 0");
        if (req.getShipDays() != null && req.getShipDays() < 0) throw new BadRequestException("shipDays must be >= 0");
        if (req.getMaxTotalQty() != null && req.getMaxTotalQty() < req.getMinTotalQty()) {
            throw new BadRequestException("maxTotalQty must be >= thresholdQty");
        }

        ProductSellWindow e = pswRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ProductSellWindow not found: " + id));

        e.setMinTotalQty(req.getMinTotalQty());
        e.setMaxTotalQty(req.getMaxTotalQty());
        e.setLeadDays(req.getLeadDays());
        e.setShipDays(req.getShipDays());
        if (req.getEnabled() != null) {
            e.setClosed(!req.getEnabled());
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
                e.getMinTotalQty(),
                e.getMaxTotalQty(),
                e.getLeadDays(),
                e.getShipDays(),
                e.isClosed()
        );
    }

    private ProductSellWindowDto.CombinedResponse toCombinedResponse(ProductSellWindow e) {
        return toCombinedResponse(e, null);
    }

    private ProductSellWindowDto.CombinedResponse toCombinedResponse(ProductSellWindow e, BatchCounterResponse counter) {
        return new ProductSellWindowDto.CombinedResponse(
                toResponse(e, counter),
                toSellWindowResponse(e.getSellWindow())
        );
    }

    private ProductSellWindowDto.CombinedResponse toCombinedResponseFromRow(ProductSellWindowRepository.ProductSellWindowWithRefsRow row, BatchCounterResponse counter) {
        ProductSellWindowDto.Response psw = new ProductSellWindowDto.Response(
                row.getId(),
                row.getProductId(),
                row.getSellWindowId(),
                row.getMinTotalQty() != null ? row.getMinTotalQty() : 0,
                qtyOrZero(counter != null ? counter.getReservedQty() : null),
                qtyOrZero(counter != null ? counter.getPaidQty() : null),
                row.getMaxTotalQty(),
                row.getLeadDays(),
                row.getShipDays(),
                Boolean.TRUE.equals(row.getIsClosed())
        );
        SellWindowDto.Response sw = new SellWindowDto.Response(
                row.getSellWindowId(),
                row.getSellWindowName(),
                row.getSellWindowStartAt(),
                row.getSellWindowEndAt(),
                row.getSellWindowTimezone(),
                row.getSellWindowStatus(),
                row.getSellWindowPaymentTtlMinutes() != null ? row.getSellWindowPaymentTtlMinutes() : 0,
                row.getSellWindowPaymentOpenedAt(),
                row.getSellWindowPaymentCloseAt(),
                row.getSellWindowPredictedPaymentDate(),
                row.getSellWindowPredictedProdDate(),
                row.getSellWindowPredictedShipDate()
        );
        return new ProductSellWindowDto.CombinedResponse(psw, sw);
    }

    private ProductSellWindowDto.Response toResponse(ProductSellWindow e, BatchCounterResponse counter) {
        ProductSellWindowDto.Response response = toResponse(e);
        response.setReservedQty(qtyOrZero(counter != null ? counter.getReservedQty() : null));
        response.setPaidQty(qtyOrZero(counter != null ? counter.getPaidQty() : null));
        return response;
    }

    private Map<String, BatchCounterResponse> loadCounterMap(List<?> sourceRows) {
        if (sourceRows == null || sourceRows.isEmpty()) {
            return Map.of();
        }

        List<BatchCounterLookupRequest.Item> items = sourceRows.stream()
                .map(this::toLookupItem)
                .filter(item -> item != null)
                .distinct()
                .toList();

        if (items.isEmpty()) {
            return Map.of();
        }

        return thresholdClient.queryBatchCounters(items).stream()
                .collect(HashMap::new, (map, counter) -> map.put(key(counter.getSellWindowId(), counter.getProductId()), counter), HashMap::putAll);
    }

    private BatchCounterLookupRequest.Item toLookupItem(Object source) {
        if (source instanceof ProductSellWindow e) {
            return new BatchCounterLookupRequest.Item(e.getSellWindow().getId(), e.getProduct().getId());
        }
        if (source instanceof ProductSellWindowRepository.ProductSellWindowWithRefsRow row) {
            return new BatchCounterLookupRequest.Item(row.getSellWindowId(), row.getProductId());
        }
        return null;
    }

    private int qtyOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String key(UUID sellWindowId, UUID productId) {
        return sellWindowId + ":" + productId;
    }

    private ProductSellWindowDto.PageItem toPageItem(ProductSellWindowRepository.ProductSellWindowWithRefsRow row) {
        return new ProductSellWindowDto.PageItem(
                row.getId(),
                row.getMinTotalQty(),
                row.getMaxTotalQty(),
                row.getLeadDays(),
                row.getShipDays(),
                Boolean.TRUE.equals(row.getIsClosed()),
                new ProductSellWindowDto.ProductInfo(
                        row.getProductId(),
                        row.getProductName(),
                        row.getProductDescription(),
                        row.getProductStatus() != null ? row.getProductStatus().name() : null,
                        row.getProductUnitPriceCents(),
                        row.getProductCurrency(),
                        row.getProductDefaultMinQty(),
                        row.getProductDefaultMaxQty(),
                        row.getProductDefaultLeadDays(),
                        row.getProductDefaultShipDays()
                ),
                new ProductSellWindowDto.SellWindowInfo(
                        row.getSellWindowId(),
                        row.getSellWindowName(),
                        row.getSellWindowStatus() != null ? row.getSellWindowStatus().name() : null,
                        row.getSellWindowStartAt(),
                        row.getSellWindowEndAt(),
                        row.getSellWindowTimezone(),
                        row.getSellWindowPaymentCloseAt(),
                        row.getSellWindowPredictedPaymentDate(),
                        row.getSellWindowPredictedProdDate(),
                        row.getSellWindowPredictedShipDate()
                )
        );
    }

    private SellWindowDto.Response toSellWindowResponse(SellWindow e) {
        return new SellWindowDto.Response(
                e.getId(),
                e.getName(),
                e.getStartAt(),
                e.getEndAt(),
                e.getTimezone(),
                e.getStatus(),
                e.getPaymentTtlMinutes(),
                e.getPaymentOpenedAt(),
                e.getPaymentCloseAt(),
                e.getPredictedPaymentDate(),
                e.getPredictedProdDate(),
                e.getPredictedShipDate()
        );
    }

    private void validateSellWindow(String name, java.time.OffsetDateTime startAt,
                                    java.time.OffsetDateTime endAt, String timezone) {
        if (name == null || name.isBlank()) throw new BadRequestException("name is required");
        if (startAt == null) throw new BadRequestException("startAt is required");
        if (endAt == null) throw new BadRequestException("endAt is required");
        if (!endAt.isAfter(startAt)) throw new BadRequestException("endAt must be after startAt");
        if (timezone == null || timezone.isBlank()) throw new BadRequestException("timezone is required");
        try {
            ZoneId.of(timezone);
        } catch (Exception e) {
            throw new BadRequestException("Invalid timezone: " + timezone);
        }
    }

    private void onProductSellWindowCreated(UUID sellWindowId, UUID productId, int thresholdQty) {

        BatchCounterRequest req = new BatchCounterRequest();
        req.setSellWindowId(sellWindowId);
        req.setProductId(productId);
        req.setThresholdQty(thresholdQty);
        req.setStatus("OPEN");

        thresholdClient.createBatchCounter(req);
    }

    private void writeOutbox(String aggregateType, UUID aggregateId, String eventType, Object payloadObj) {
        try {
            OutboxEvent evt = new OutboxEvent();
            evt.setId(UUID.randomUUID());
            evt.setAggregateType(aggregateType);
            evt.setAggregateId(aggregateId);
            evt.setIdempotencyKey(eventType + ":" + aggregateId); // 簡單的 idempotency key，實際可根據需求調整
            evt.setEventType(eventType);
            evt.setPayload(objectMapper.valueToTree(payloadObj));
            evt.setStatus(OutboxEventStatus.PENDING);
            outboxEventRepository.save(evt);
        } catch (Exception e) {
            // 讓 transaction rollback，確保「業務寫入 + outbox」同生共死
            throw new RuntimeException("Failed to write outbox event", e);
        }
    }

}
