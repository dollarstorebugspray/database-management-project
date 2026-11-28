package com.grocerystore.backend.dto;

public record OrderSearch(
        Integer storeId,
        String status,
        String search,
        Integer limit) {
}
