package com.grocerystore.backend.dto;

public record StoreResponse(
        Integer storeId,
        String street,
        String city,
        String state,
        String zipCode,
        String manager,
        Integer regionId,
        String regionName,
        Integer salespersonCount) {
}
