package com.merdeleine.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 目前只允許更新數量；變更後會依 delta 發送事件到 threshold-service。
 */
public record UpdateOrderRequest(
		@NotNull @Min(1) Integer quantity
) {}
