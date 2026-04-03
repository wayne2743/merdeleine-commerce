package com.merdeleine.catalog.service;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Component
public class SellWindowPlanner {

    public record Plan(
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            String timezone,
            int paymentTtlMinutes
    ) {}

    // MVP：先固定規則，之後你可改成依 leadDays/shipDays/出貨日等規劃
    public Plan plan() {
        ZoneId tz = ZoneId.of("Asia/Taipei");
        OffsetDateTime now = OffsetDateTime.now(tz);
        return planFrom(now);
    }

    public Plan planFrom(OffsetDateTime startAt) {
        ZoneId tz = ZoneId.of("Asia/Taipei");
        OffsetDateTime effectiveStartAt = startAt != null ? startAt : OffsetDateTime.now(tz);
        OffsetDateTime endAt = effectiveStartAt.plusDays(2);   // 例：開團 2 天後截止
        int paymentTtlMinutes = 60 * 24;          // 例：24 小時（confirm 後才開放付款）
        return new Plan(effectiveStartAt, endAt, tz.getId(), paymentTtlMinutes);
    }
}