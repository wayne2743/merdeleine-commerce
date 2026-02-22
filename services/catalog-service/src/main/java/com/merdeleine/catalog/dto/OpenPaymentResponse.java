package com.merdeleine.catalog.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class OpenPaymentResponse {
    private UUID sellWindowId;
    private OffsetDateTime paymentOpenedAt;
    private OffsetDateTime paymentCloseAt;
    private String status;
    private long version;

    public OpenPaymentResponse(UUID sellWindowId, OffsetDateTime paymentOpenedAt, OffsetDateTime paymentCloseAt, String status, long version) {
        this.sellWindowId = sellWindowId;
        this.paymentOpenedAt = paymentOpenedAt;
        this.paymentCloseAt = paymentCloseAt;
        this.status = status;
        this.version = version;
    }

    public UUID getSellWindowId() { return sellWindowId; }
    public OffsetDateTime getPaymentOpenedAt() { return paymentOpenedAt; }
    public OffsetDateTime getPaymentCloseAt() { return paymentCloseAt; }
    public String getStatus() { return status; }
    public long getVersion() { return version; }
}