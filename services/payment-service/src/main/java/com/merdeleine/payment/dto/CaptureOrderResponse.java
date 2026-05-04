package com.merdeleine.payment.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record CaptureOrderResponse(
        String orderId,
        String status,
        JsonNode raw
) {
}