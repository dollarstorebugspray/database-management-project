package com.grocerystore.backend.dto;

import java.math.BigDecimal;

public record InventoryItemResponse(
        Integer productId,
        String productName,
        String brand,
        BigDecimal price,
        Integer categoryId,
        String categoryName,
        Integer quantity,
        boolean carried,
        boolean lowStock,
        String imageUrl,
        BigDecimal lastUnitCost,
        String lastSupplier) {
}
