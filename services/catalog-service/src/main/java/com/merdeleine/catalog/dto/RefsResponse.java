package com.merdeleine.catalog.dto;

import java.util.UUID;

public record RefsResponse(
        UUID productId,
        String productName,
        UUID sellWindowId,
        String sellWindowName
) {}
