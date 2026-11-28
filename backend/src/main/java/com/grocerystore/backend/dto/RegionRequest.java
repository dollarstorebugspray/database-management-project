package com.grocerystore.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegionRequest(
        @NotBlank @Size(max = 50) String regionName,
        @Size(max = 50) String regionManager) {
}
