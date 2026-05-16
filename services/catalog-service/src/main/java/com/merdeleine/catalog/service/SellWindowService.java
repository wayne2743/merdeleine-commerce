package com.merdeleine.catalog.service;


import com.merdeleine.catalog.client.ThresholdServiceClient;
import com.merdeleine.catalog.dto.NextGroupOpenAtResponse;
import com.merdeleine.catalog.dto.OpenPaymentRequest;
import com.merdeleine.catalog.dto.OpenPaymentResponse;
import com.merdeleine.catalog.dto.SellWindowDto;
import com.merdeleine.catalog.entity.SellWindow;
import com.merdeleine.catalog.enums.SellWindowStatus;
import com.merdeleine.catalog.exception.BadRequestException;
import com.merdeleine.catalog.exception.NotFoundException;
import com.merdeleine.catalog.repository.ProductSellWindowRepository;
import com.merdeleine.catalog.repository.SellWindowRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SellWindowService {

    private static final Logger log = LoggerFactory.getLogger(SellWindowService.class);

    private final SellWindowRepository sellWindowRepository;
    private final ProductSellWindowRepository productSellWindowRepository;
    private final ThresholdServiceClient thresholdServiceClient;
    private final SellWindowExpireService sellWindowExpireService;

    public SellWindowService(SellWindowRepository sellWindowRepository,
                             ProductSellWindowRepository productSellWindowRepository,
                             ThresholdServiceClient thresholdServiceClient,
                             SellWindowExpireService sellWindowExpireService) {
        this.sellWindowRepository = sellWindowRepository;
        this.productSellWindowRepository = productSellWindowRepository;
        this.thresholdServiceClient = thresholdServiceClient;
        this.sellWindowExpireService = sellWindowExpireService;
    }

    @Transactional
    public SellWindowDto.Response create(SellWindowDto.CreateRequest req) {
        validate(req.getName(), req.getStartAt(), req.getEndAt(), req.getTimezone());

        if (sellWindowRepository.existsByName(req.getName())) {
            throw new BadRequestException("SellWindow name already exists: " + req.getName());
        }

        SellWindow e = new SellWindow();
        e.setName(req.getName());
        e.setStartAt(req.getStartAt());
        e.setEndAt(req.getEndAt());
        e.setTimezone(req.getTimezone());
        e.setPredictedPaymentDate(req.getPredictedPaymentDate());
        e.setPredictedProdDate(req.getPredictedProdDate());
        e.setPredictedShipDate(req.getPredictedShipDate());

        return toResponse(sellWindowRepository.save(e));
    }

    @Transactional(readOnly = true)
    public SellWindowDto.Response get(UUID id) {
        return toResponse(sellWindowRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("SellWindow not found: " + id)));
    }

    @Transactional(readOnly = true)
    public List<SellWindowDto.Response> list() {
        return sellWindowRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SellWindowNameRef> batchGetNames(List<UUID> sellWindowIds) {
        if (sellWindowIds == null || sellWindowIds.isEmpty()) {
            return List.of();
        }

        List<UUID> uniqueSellWindowIds = new LinkedHashSet<>(sellWindowIds).stream().toList();
        Map<UUID, String> idNameMap = sellWindowRepository.findAllById(uniqueSellWindowIds).stream()
                .collect(Collectors.toMap(SellWindow::getId, SellWindow::getName, (a, b) -> a));

        return uniqueSellWindowIds.stream()
                .map(id -> new SellWindowNameRef(id, idNameMap.get(id)))
                .toList();
    }

    @Transactional(readOnly = true)
    public NextGroupOpenAtResponse getNextGroupOpenAt() {
        OffsetDateTime dbMaxEndAt = sellWindowRepository.findMaxEndAt();
        OffsetDateTime now = OffsetDateTime.now();

        // 基準點：取 DB 最大 endAt 與現在兩者的較晚值，確保不早於現在
        OffsetDateTime base = (dbMaxEndAt == null || dbMaxEndAt.isBefore(now)) ? now : dbMaxEndAt;

        // 調整到下一個禮拜一（若 base 本身已是禮拜一，則推到再下一個禮拜一）
        OffsetDateTime nextMonday = base
                .with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        int weekOfYear = nextMonday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);

        return new NextGroupOpenAtResponse(nextMonday, weekOfYear);
    }

    @Transactional
    public SellWindowDto.Response update(UUID id, SellWindowDto.UpdateRequest req) {
        validate(req.getName(), req.getStartAt(), req.getEndAt(), req.getTimezone());

        SellWindow e = sellWindowRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("SellWindow not found: " + id));

        if (!e.getName().equals(req.getName()) && sellWindowRepository.existsByName(req.getName())) {
            throw new BadRequestException("SellWindow name already exists: " + req.getName());
        }

        e.setName(req.getName());
        e.setStartAt(req.getStartAt());
        e.setEndAt(req.getEndAt());
        e.setTimezone(req.getTimezone());
        e.setPredictedPaymentDate(req.getPredictedPaymentDate());
        e.setPredictedProdDate(req.getPredictedProdDate());
        e.setPredictedShipDate(req.getPredictedShipDate());

        return toResponse(sellWindowRepository.save(e));
    }

    @Transactional
    public void delete(UUID id) {
        if (!sellWindowRepository.existsById(id)) {
            throw new NotFoundException("SellWindow not found: " + id);
        }

        // Align delete flow with POST /internal/sell-windows/{sellWindowId}/close behavior.
        sellWindowExpireService.closeBySellWindowId(id);

        // 1) 刪除 threshold-service 的 batch_counter（及 cascade counter_event_log）
        thresholdServiceClient.deleteBatchCountersBySellWindowId(id.toString());
        // 2) 刪除 catalog DB 的 product_sell_window（FK 保護）
        productSellWindowRepository.deleteBySellWindowId(id);
        // 3) 刪除 sell_window
        sellWindowRepository.deleteById(id);
    }




    private void validate(String name, OffsetDateTime startAt, OffsetDateTime endAt, String timezone) {
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

    private SellWindowDto.Response toResponse(SellWindow e) {
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

    @Transactional
    public OpenPaymentResponse openPayment(UUID sellWindowId, OpenPaymentRequest req) {
        SellWindow sw = sellWindowRepository.findByIdForUpdate(sellWindowId)
                .orElseThrow(() -> new EntityNotFoundException("SellWindow not found: " + sellWindowId));

        // ---- 冪等：已開放付款就直接回傳（不重算時間） ----
        if (sw.getPaymentOpenedAt() != null && sw.getPaymentCloseAt() != null) {
            return toPaymentResponse(sw);
        }

        int ttl = sw.getPaymentTtlMinutes();
        if (req != null && req.getOverrideTtlMinutes() != null) {
            ttl = req.getOverrideTtlMinutes();
        }
        if (ttl <= 0 || ttl > 60 * 24 * 30) { // 0 < ttl <= 30 days
            throw new IllegalArgumentException("Invalid ttlMinutes: " + ttl);
        }

        // ---- 以 sellWindow.timezone 計算 now（存 OffsetDateTime）----
        ZoneId zoneId = ZoneId.of(sw.getTimezone());
        ZonedDateTime nowZdt = ZonedDateTime.now(zoneId);
        OffsetDateTime openedAt = nowZdt.toOffsetDateTime();
        OffsetDateTime closeAt = nowZdt.plusMinutes(ttl).toOffsetDateTime();

        sw.setPaymentOpenedAt(openedAt);
        sw.setPaymentCloseAt(closeAt);
        sw.setStatus(SellWindowStatus.FINISHED);

        // save 非必須（JPA dirty checking），但寫出來更清楚
        sellWindowRepository.save(sw);

        return toPaymentResponse(sw);
    }

    private OpenPaymentResponse toPaymentResponse(SellWindow sw) {
        return new OpenPaymentResponse(
                sw.getId(),
                sw.getPaymentOpenedAt(),
                sw.getPaymentCloseAt(),
                sw.getStatus().name(),
                sw.getVersion()
        );
    }

    public record SellWindowNameRef(UUID sellWindowId, String sellWindowName) {
    }
}
