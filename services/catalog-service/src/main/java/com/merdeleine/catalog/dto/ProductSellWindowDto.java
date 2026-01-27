package com.merdeleine.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public final class ProductSellWindowDto {

    private ProductSellWindowDto() {}

    public static final class CreateRequest {
        @NotNull
        private UUID productId;

        @NotNull
        private UUID sellWindowId;

        @Min(1)
        private int thresholdQty;

        private Integer maxTotalQty;

        @Min(0)
        private Integer leadDays;

        @Min(0)
        private Integer shipDays;

        private Boolean enabled;

        public CreateRequest() {}

        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }

        public UUID getSellWindowId() { return sellWindowId; }
        public void setSellWindowId(UUID sellWindowId) { this.sellWindowId = sellWindowId; }

        public int getThresholdQty() { return thresholdQty; }
        public void setThresholdQty(int thresholdQty) { this.thresholdQty = thresholdQty; }

        public Integer getMaxTotalQty() { return maxTotalQty; }
        public void setMaxTotalQty(Integer maxTotalQty) { this.maxTotalQty = maxTotalQty; }

        public Integer getLeadDays() { return leadDays; }
        public void setLeadDays(Integer leadDays) { this.leadDays = leadDays; }

        public Integer getShipDays() { return shipDays; }
        public void setShipDays(Integer shipDays) { this.shipDays = shipDays; }

        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    }

    public static final class UpdateRequest {
        @Min(1)
        private int thresholdQty;

        private Integer maxTotalQty;

        @Min(0)
        private Integer leadDays;

        @Min(0)
        private Integer shipDays;

        private Boolean enabled;

        public UpdateRequest() {}

        public int getThresholdQty() { return thresholdQty; }
        public void setThresholdQty(int thresholdQty) { this.thresholdQty = thresholdQty; }

        public Integer getMaxTotalQty() { return maxTotalQty; }
        public void setMaxTotalQty(Integer maxTotalQty) { this.maxTotalQty = maxTotalQty; }

        public Integer getLeadDays() { return leadDays; }
        public void setLeadDays(Integer leadDays) { this.leadDays = leadDays; }

        public Integer getShipDays() { return shipDays; }
        public void setShipDays(Integer shipDays) { this.shipDays = shipDays; }

        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    }

    public static final class Response {
        private UUID id;
        private UUID productId;
        private UUID sellWindowId;

        private int thresholdQty;
        private Integer maxTotalQty;
        private Integer leadDays;
        private Integer shipDays;
        private boolean enabled;

        public Response() {}

        public Response(UUID id, UUID productId, UUID sellWindowId,
                        int thresholdQty, Integer maxTotalQty, Integer leadDays, Integer shipDays, boolean enabled) {
            this.id = id;
            this.productId = productId;
            this.sellWindowId = sellWindowId;
            this.thresholdQty = thresholdQty;
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

        public Integer getMaxTotalQty() { return maxTotalQty; }
        public void setMaxTotalQty(Integer maxTotalQty) { this.maxTotalQty = maxTotalQty; }

        public Integer getLeadDays() { return leadDays; }
        public void setLeadDays(Integer leadDays) { this.leadDays = leadDays; }

        public Integer getShipDays() { return shipDays; }
        public void setShipDays(Integer shipDays) { this.shipDays = shipDays; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
