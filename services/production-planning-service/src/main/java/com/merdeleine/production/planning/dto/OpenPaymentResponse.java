package com.merdeleine.production.planning.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class OpenPaymentResponse {
    private UUID sellWindowId;
    private OffsetDateTime paymentOpenedAt;
    private OffsetDateTime paymentCloseAt;
    private String status;
    private long version;

    public UUID getSellWindowId() { return sellWindowId; }
    public void setSellWindowId(UUID sellWindowId) { this.sellWindowId = sellWindowId; }

    public OffsetDateTime getPaymentOpenedAt() { return paymentOpenedAt; }
    public void setPaymentOpenedAt(OffsetDateTime paymentOpenedAt) { this.paymentOpenedAt = paymentOpenedAt; }

    public OffsetDateTime getPaymentCloseAt() { return paymentCloseAt; }
    public void setPaymentCloseAt(OffsetDateTime paymentCloseAt) { this.paymentCloseAt = paymentCloseAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
