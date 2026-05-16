package com.merdeleine.catalog.dto.threshold;

import java.util.List;
import java.util.UUID;

public class BatchCounterLookupRequest {

    private List<Item> items;

    public BatchCounterLookupRequest() {}

    public BatchCounterLookupRequest(List<Item> items) {
        this.items = items;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public static class Item {
        private UUID sellWindowId;
        private UUID productId;

        public Item() {}

        public Item(UUID sellWindowId, UUID productId) {
            this.sellWindowId = sellWindowId;
            this.productId = productId;
        }

        public UUID getSellWindowId() {
            return sellWindowId;
        }

        public void setSellWindowId(UUID sellWindowId) {
            this.sellWindowId = sellWindowId;
        }

        public UUID getProductId() {
            return productId;
        }

        public void setProductId(UUID productId) {
            this.productId = productId;
        }
    }
}

