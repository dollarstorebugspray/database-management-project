package com.grocerystore.backend.dto;

public record RegionResponse(
        Integer regionId,
        String regionName,
        String regionManager,
        Integer storeCount) {
}
