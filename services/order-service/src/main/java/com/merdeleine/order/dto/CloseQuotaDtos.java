package com.merdeleine.order.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class CloseQuotaDtos {

    public record CloseQuotaRequest(
            @NotNull UUID sellWindowId,
            @NotNull UUID productId,
            UUID reasonEventId,
            String reason
    ) {}

    public record CloseQuotaResponse(
            UUID sellWindowId,
            UUID productId,
            boolean closed,     // 這次呼叫是否真的把狀態改掉（OPEN->CLOSED）
            String status       // 回傳目標狀態（CLOSED）
    ) {}
}
