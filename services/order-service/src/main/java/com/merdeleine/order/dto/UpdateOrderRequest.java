package com.merdeleine.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 更新訂單：可更新數量 及 收貨地址；變更數量後會依 delta 發送事件到 threshold-service。
 */
public record UpdateOrderRequest(
		@Min(1) Integer quantity,
		@Size(max = 1000) String shippingAddress
) {}
