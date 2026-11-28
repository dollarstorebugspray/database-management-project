package com.grocerystore.backend.dto;

import java.math.BigDecimal;

public record ProductResponse(
        Integer productId,
        String productName,
        String brand,
        BigDecimal price,
        Integer categoryId,
        String categoryName,
        Integer quantity,
        String imageUrl) {
}
