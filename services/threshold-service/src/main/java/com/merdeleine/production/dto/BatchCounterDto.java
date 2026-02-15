package com.merdeleine.production.dto;

import com.merdeleine.production.enums.CounterStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class BatchCounterDto {

    private BatchCounterDto() {}

    public static final class CreateRequest {
        @NotNull
        private UUID sellWindowId;

        @NotNull
        private UUID productId;

        @NotNull
        @Min(1)
        private Integer thresholdQty;

        @NotNull
        private CounterStatus status;

        public CreateRequest() {}

        public UUID getSellWindowId() { return sellWindowId; }
        public void setSellWindowId(UUID sellWindowId) { this.sellWindowId = sellWindowId; }

        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }

        public Integer getThresholdQty() { return thresholdQty; }
        public void setThresholdQty(Integer thresholdQty) { this.thresholdQty = thresholdQty; }

        public CounterStatus getStatus() { return status; }
        public void setStatus(CounterStatus status) { this.status = status; }
    }

    public static final class UpdateRequest {
        @NotNull
        @Min(1)
        private Integer thresholdQty;

        @NotNull
        private CounterStatus status;

        public UpdateRequest() {}

        public Integer getThresholdQty() { return thresholdQty; }
        public void setThresholdQty(Integer thresholdQty) { this.thresholdQty = thresholdQty; }

        public CounterStatus getStatus() { return status; }
        public void setStatus(CounterStatus status) { this.status = status; }
    }

    public static final class Response {
        private UUID id;
        private UUID sellWindowId;
        private UUID productId;
        private Integer paidQty;
        private Integer thresholdQty;
        private CounterStatus status;
        private OffsetDateTime reachedAt;
        private UUID reachedEventId;
        private OffsetDateTime updatedAt;

        public Response() {}

        public Response(UUID id, UUID sellWindowId, UUID productId, Integer paidQty, Integer thresholdQty,
                        CounterStatus status, OffsetDateTime reachedAt, UUID reachedEventId, OffsetDateTime updatedAt) {
            this.id = id;
            this.sellWindowId = sellWindowId;
            this.productId = productId;
            this.paidQty = paidQty;
            this.thresholdQty = thresholdQty;
            this.status = status;
            this.reachedAt = reachedAt;
            this.reachedEventId = reachedEventId;
            this.updatedAt = updatedAt;
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }

        public UUID getSellWindowId() { return sellWindowId; }
        public void setSellWindowId(UUID sellWindowId) { this.sellWindowId = sellWindowId; }

        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }

        public Integer getPaidQty() { return paidQty; }
        public void setPaidQty(Integer paidQty) { this.paidQty = paidQty; }

        public Integer getThresholdQty() { return thresholdQty; }
        public void setThresholdQty(Integer thresholdQty) { this.thresholdQty = thresholdQty; }

        public CounterStatus getStatus() { return status; }
        public void setStatus(CounterStatus status) { this.status = status; }

        public OffsetDateTime getReachedAt() { return reachedAt; }
        public void setReachedAt(OffsetDateTime reachedAt) { this.reachedAt = reachedAt; }

        public UUID getReachedEventId() { return reachedEventId; }
        public void setReachedEventId(UUID reachedEventId) { this.reachedEventId = reachedEventId; }

        public OffsetDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    /** 用來做「加總 paidQty」+ 寫入事件 log 的 request */
    public static final class ApplyPaidEventRequest {
        @NotNull
        private String sourceEventType;

        @NotNull
        private UUID sourceEventId;

        @NotNull
        @Min(1)
        private Integer deltaQty;

        public ApplyPaidEventRequest() {}

        public String getSourceEventType() { return sourceEventType; }
        public void setSourceEventType(String sourceEventType) { this.sourceEventType = sourceEventType; }

        public UUID getSourceEventId() { return sourceEventId; }
        public void setSourceEventId(UUID sourceEventId) { this.sourceEventId = sourceEventId; }

        public Integer getDeltaQty() { return deltaQty; }
        public void setDeltaQty(Integer deltaQty) { this.deltaQty = deltaQty; }
    }
}
