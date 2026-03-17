package com.merdeleine.catalog.exception;

import java.util.UUID;

public class ProductImageNotFoundException extends NotFoundException {

    public ProductImageNotFoundException(UUID id) {
        super("ProductImage not found with id: " + id);
    }

    public ProductImageNotFoundException(UUID productId, UUID imageId) {
        super("ProductImage not found with id: " + imageId + " for product: " + productId);
    }
}

