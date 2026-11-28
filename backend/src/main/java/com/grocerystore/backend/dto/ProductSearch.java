package com.grocerystore.backend.dto;

import java.math.BigDecimal;

public record ProductSearch(
        Integer storeId,
        String search,
        Integer categoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        boolean inStockOnly,
        String sort,
        Integer limit) {
}
