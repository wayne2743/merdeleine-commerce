package com.merdeleine.order.mapper;


import com.merdeleine.order.dto.OrderItemResponse;
import com.merdeleine.order.dto.OrderResponse;
import com.merdeleine.order.entity.*;

import java.util.List;

public class OrderResponseMapper {

    public OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderNo(),
                order.getCustomerId(),
                order.getSellWindowId(),
                order.getStatus().name(),
                order.getTotalAmountCents(),
                order.getCurrency(),
                order.getContactName(),
                order.getContactPhone(),
                order.getContactEmail(),
                order.getShippingAddress(),
                // createdAt/updatedAt 在 Entity 中有，但這裡沒 getter（你有的話接上）
                null,
                null,
                items
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getVariantId(),
                item.getQuantity(),
                item.getUnitPriceCents(),
                item.getSubtotalCents()
        );
    }
}
