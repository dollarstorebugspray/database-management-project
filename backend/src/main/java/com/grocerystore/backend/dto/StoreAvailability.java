package com.grocerystore.backend.dto;

public record StoreAvailability(
        Integer storeId,
        String street,
        String city,
        String state,
        Integer quantity) {
}
