package com.merdeleine.order.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StorePickupLocationResponse(
        UUID id,
        String name,
        String address,
        String contactPhone,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}

