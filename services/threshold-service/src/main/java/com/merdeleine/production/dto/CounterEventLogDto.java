package com.merdeleine.production.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class CounterEventLogDto {

    private CounterEventLogDto() {}

    public static final class Response {
        private UUID id;
        private UUID counterId;
        private String sourceEventType;
        private UUID sourceEventId;
        private Integer deltaQty;
        private OffsetDateTime createdAt;

        public Response() {}

        public Response(UUID id, UUID counterId, String sourceEventType, UUID sourceEventId,
                        Integer deltaQty, OffsetDateTime createdAt) {
            this.id = id;
            this.counterId = counterId;
            this.sourceEventType = sourceEventType;
            this.sourceEventId = sourceEventId;
            this.deltaQty = deltaQty;
            this.createdAt = createdAt;
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }

        public UUID getCounterId() { return counterId; }
        public void setCounterId(UUID counterId) { this.counterId = counterId; }

        public String getSourceEventType() { return sourceEventType; }
        public void setSourceEventType(String sourceEventType) { this.sourceEventType = sourceEventType; }

        public UUID getSourceEventId() { return sourceEventId; }
        public void setSourceEventId(UUID sourceEventId) { this.sourceEventId = sourceEventId; }

        public Integer getDeltaQty() { return deltaQty; }
        public void setDeltaQty(Integer deltaQty) { this.deltaQty = deltaQty; }

        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    }
}
