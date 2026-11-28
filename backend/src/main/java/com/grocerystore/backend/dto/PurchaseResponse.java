package com.grocerystore.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseResponse(
        Integer purchaseId,
        LocalDate purchaseDate,
        Integer storeId,
        String storeCity,
        Integer productId,
        String productName,
        Integer quantity,
        BigDecimal unitCost,
        BigDecimal totalCost,
        String supplier) {
}
