package com.merdeleine.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

import java.time.OffsetDateTime;

import java.util.UUID;

public final class ProductSellWindowDto {

    private ProductSellWindowDto() {}

    public static final class CreateRequest {
        @NotNull
        private UUID productId;

        @NotNull
        private UUID sellWindowId;

        @Min(1)
        private int minTotalQty;

        private Integer maxTotalQty;

        @Min(0)
        private Integer leadDays;

        @Min(0)
        private Integer shipDays;

        private Boolean isClosed;

        public CreateRequest() {}

        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }

        public UUID getSellWindowId() { return sellWindowId; }
        public void setSellWindowId(UUID sellWindowId) { this.sellWindowId = sellWindowId; }

        public int getMinTotalQty() {
            return minTotalQty;
        }

        public void setMinTotalQty(int minTotalQty) {
            this.minTotalQty = minTotalQty;
        }

        public Integer getMaxTotalQty() { return maxTotalQty; }
        public void setMaxTotalQty(Integer maxTotalQty) { this.maxTotalQty = maxTotalQty; }

        public Integer getLeadDays() { return leadDays; }
        public void setLeadDays(Integer leadDays) { this.leadDays = leadDays; }

        public Integer getShipDays() { return shipDays; }
        public void setShipDays(Integer shipDays) { this.shipDays = shipDays; }

        public Boolean isClosed() { return isClosed; }
        public void setIsClosed(Boolean isClosed) { this.isClosed = isClosed; }
    }

    public static final class UpdateRequest {
        @Min(1)
        private int minTotalQty;

        private Integer maxTotalQty;

        @Min(0)
        private Integer leadDays;

        @Min(0)
        private Integer shipDays;

        private Boolean isClosed;

        public UpdateRequest() {}

        public int getMinTotalQty() { return minTotalQty; }
        public void setMinTotalQty(int minTotalQty) { this.minTotalQty = minTotalQty; }

        public Integer getMaxTotalQty() { return maxTotalQty; }
        public void setMaxTotalQty(Integer maxTotalQty) { this.maxTotalQty = maxTotalQty; }

        public Integer getLeadDays() { return leadDays; }
        public void setLeadDays(Integer leadDays) { this.leadDays = leadDays; }

        public Integer getShipDays() { return shipDays; }
        public void setShipDays(Integer shipDays) { this.shipDays = shipDays; }

        public Boolean getEnabled() { return isClosed; }
        public void setEnabled(Boolean isClosed) { this.isClosed = isClosed; }
    }

    public static final class CreateWithSellWindowRequest {
        @NotNull
        private UUID productId;

        @Valid
        @NotNull
        private SellWindowDto.CreateRequest sellWindow;

        @Min(1)
        private Integer minTotalQty;

        private Integer maxTotalQty;

        @Min(0)
        private Integer leadDays;

        @Min(0)
        private Integer shipDays;

        private Boolean isClosed;

        public CreateWithSellWindowRequest() {}

        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }

        public SellWindowDto.CreateRequest getSellWindow() { return sellWindow; }
        public void setSellWindow(SellWindowDto.CreateRequest sellWindow) { this.sellWindow = sellWindow; }

        public Integer getMinTotalQty() { return minTotalQty; }
        public void setMinTotalQty(Integer minTotalQty) { this.minTotalQty = minTotalQty; }

        public Integer getMaxTotalQty() { return maxTotalQty; }
        public void setMaxTotalQty(Integer maxTotalQty) { this.maxTotalQty = maxTotalQty; }

        public Integer getLeadDays() { return leadDays; }
        public void setLeadDays(Integer leadDays) { this.leadDays = leadDays; }

        public Integer getShipDays() { return shipDays; }
        public void setShipDays(Integer shipDays) { this.shipDays = shipDays; }

        public Boolean getIsClosed() { return isClosed; }
        public void setIsClosed(Boolean isClosed) { this.isClosed = isClosed; }
    }

    public static final class Response {
        private UUID id;
        private UUID productId;
        private String productiName;
        private UUID sellWindowId;
        private String sellWindowName;

        private Integer thresholdQty;
        private Integer reservedQty;
        private Integer paidQty;
        private Integer maxTotalQty;
        private Integer leadDays;
        private Integer shipDays;
        private boolean enabled;

        public Response() {}

        public Response(UUID id, UUID productId, UUID sellWindowId,
                        int thresholdQty, Integer maxTotalQty, Integer leadDays, Integer shipDays, boolean enabled) {
            this(id, productId, sellWindowId, thresholdQty, null, null, maxTotalQty, leadDays, shipDays, enabled);
        }

        public Response(UUID id, UUID productId, UUID sellWindowId,
                        int thresholdQty, Integer reservedQty, Integer paidQty,
                        Integer maxTotalQty, Integer leadDays, Integer shipDays, boolean enabled) {
            this.id = id;
            this.productId = productId;
            this.sellWindowId = sellWindowId;
            this.thresholdQty = thresholdQty;
            this.reservedQty = reservedQty;
            this.paidQty = paidQty;
            this.maxTotalQty = maxTotalQty;
            this.leadDays = leadDays;
            this.shipDays = shipDays;
            this.enabled = enabled;
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }

        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }

        public UUID getSellWindowId() { return sellWindowId; }
        public void setSellWindowId(UUID sellWindowId) { this.sellWindowId = sellWindowId; }

        public int getThresholdQty() { return thresholdQty; }
        public void setThresholdQty(int thresholdQty) { this.thresholdQty = thresholdQty; }

        public Integer getReservedQty() { return reservedQty; }
        public void setReservedQty(Integer reservedQty) { this.reservedQty = reservedQty; }

        public Integer getPaidQty() { return paidQty; }
        public void setPaidQty(Integer paidQty) { this.paidQty = paidQty; }

        public Integer getMaxTotalQty() { return maxTotalQty; }
        public void setMaxTotalQty(Integer maxTotalQty) { this.maxTotalQty = maxTotalQty; }

        public Integer getLeadDays() { return leadDays; }
        public void setLeadDays(Integer leadDays) { this.leadDays = leadDays; }

        public Integer getShipDays() { return shipDays; }
        public void setShipDays(Integer shipDays) { this.shipDays = shipDays; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public record CombinedResponse(
            Response productSellWindow,
            SellWindowDto.Response sellWindow
    ) {}

    public record PageItem(
            UUID id,
            Integer minTotalQty,
            Integer maxTotalQty,
            Integer leadDays,
            Integer shipDays,
            boolean isClosed,
            ProductInfo product,
            SellWindowInfo sellWindow
    ) {}

    public record ProductInfo(
            UUID id,
            String name,
            String description,
            String status,
            Integer unitPriceCents,
            String currency,
            Integer defaultMinQty,
            Integer defaultMaxQty,
            Integer defaultLeadDays,
            Integer defaultShipDays
    ) {}

    public record SellWindowInfo(
            UUID id,
            String name,
            String status,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            String timezone,
            OffsetDateTime paymentCloseAt,
            OffsetDateTime predictedPaymentDate,
            OffsetDateTime predictedProdDate,
            OffsetDateTime predictedShipDate
    ) {}
}
