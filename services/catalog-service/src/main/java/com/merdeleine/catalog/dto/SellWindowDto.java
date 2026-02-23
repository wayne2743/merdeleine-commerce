package com.merdeleine.catalog.dto;

import com.merdeleine.catalog.enums.SellWindowStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class SellWindowDto {

    private SellWindowDto() {}

    public static final class CreateRequest {
        @NotBlank
        private String name;

        @NotNull
        private OffsetDateTime startAt;

        @NotNull
        private OffsetDateTime endAt;

        @NotBlank
        private String timezone;

        public CreateRequest() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public OffsetDateTime getStartAt() { return startAt; }
        public void setStartAt(OffsetDateTime startAt) { this.startAt = startAt; }

        public OffsetDateTime getEndAt() { return endAt; }
        public void setEndAt(OffsetDateTime endAt) { this.endAt = endAt; }

        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }
    }

    public static final class UpdateRequest {
        @NotBlank
        private String name;

        @NotNull
        private OffsetDateTime startAt;

        @NotNull
        private OffsetDateTime endAt;

        @NotBlank
        private String timezone;

        public UpdateRequest() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public OffsetDateTime getStartAt() { return startAt; }
        public void setStartAt(OffsetDateTime startAt) { this.startAt = startAt; }

        public OffsetDateTime getEndAt() { return endAt; }
        public void setEndAt(OffsetDateTime endAt) { this.endAt = endAt; }

        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }
    }

    public static final class Response {
        private UUID id;
        private String name;
        private OffsetDateTime startAt;
        private OffsetDateTime endAt;
        private String timezone;
        private SellWindowStatus status;
        private int paymentTtlMinutes;
        private OffsetDateTime paymentOpenAt;
        private OffsetDateTime paymentCloseAt;


        public Response() {}

        public Response(UUID id, String name, OffsetDateTime startAt, OffsetDateTime endAt, String timezone, SellWindowStatus status, int paymentTtlMinutes, OffsetDateTime paymentOpenAt, OffsetDateTime paymentCloseAt) {
            this.id = id;
            this.name = name;
            this.startAt = startAt;
            this.endAt = endAt;
            this.timezone = timezone;
            this.status = status;
            this.paymentTtlMinutes = paymentTtlMinutes;
            this.paymentOpenAt = paymentOpenAt;
            this.paymentCloseAt = paymentCloseAt;
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public OffsetDateTime getStartAt() { return startAt; }
        public void setStartAt(OffsetDateTime startAt) { this.startAt = startAt; }

        public OffsetDateTime getEndAt() { return endAt; }
        public void setEndAt(OffsetDateTime endAt) { this.endAt = endAt; }

        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }

        public SellWindowStatus getStatus() {
            return status;
        }

        public void setStatus(SellWindowStatus status) {
            this.status = status;
        }

        public int getPaymentTtlMinutes() {
            return paymentTtlMinutes;
        }

        public void setPaymentTtlMinutes(int paymentTtlMinutes) {
            this.paymentTtlMinutes = paymentTtlMinutes;
        }

        public OffsetDateTime getPaymentOpenAt() {
            return paymentOpenAt;
        }

        public void setPaymentOpenAt(OffsetDateTime paymentOpenAt) {
            this.paymentOpenAt = paymentOpenAt;
        }

        public OffsetDateTime getPaymentCloseAt() {
            return paymentCloseAt;
        }

        public void setPaymentCloseAt(OffsetDateTime paymentCloseAt) {
            this.paymentCloseAt = paymentCloseAt;
        }
    }
}
