package com.grocerystore.backend.dto;

import java.util.List;

public record CreateOrderRequest(
        Integer orderId,
        Integer customerId,
        Integer storeId,
        Integer salespersonId,
        String paymentMethod,
        List<OrderItemRequest> items
) {
    public record OrderItemRequest(
            Integer productId,
            Integer quantity
    ) {
    }
}
