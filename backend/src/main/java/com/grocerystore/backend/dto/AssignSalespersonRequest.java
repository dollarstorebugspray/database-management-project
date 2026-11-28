package com.grocerystore.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AssignSalespersonRequest(
        @NotNull(message = "salespersonId is required")
        @Min(value = 1, message = "salespersonId must be at least 1")
        Integer salespersonId) {
}
