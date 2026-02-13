package com.merdeleine.notification.dto;

import java.util.UUID;

public record RefsResponse(
        UUID productId,
        String productName,
        UUID sellWindowId,
        String sellWindowName
) {}
