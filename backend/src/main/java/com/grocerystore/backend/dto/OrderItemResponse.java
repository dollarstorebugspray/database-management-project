package com.grocerystore.backend.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Integer productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {
}
