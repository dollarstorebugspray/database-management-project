package com.grocerystore.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RestockRequest(
        @NotNull @Min(1) Integer storeId,
        @NotNull @Min(1) Integer productId,
        @NotNull @Min(value = 1, message = "must be at least 1")
        @Max(value = 10000, message = "must be 10000 or less")
        Integer quantity,
        @NotNull(message = "is required")
        @DecimalMin(value = "0.00", message = "must be 0 or more")
        @DecimalMax(value = "9999.99", message = "must be 9999.99 or less")
        @Digits(integer = 4, fraction = 2, message = "must have at most 2 decimal places")
        BigDecimal unitCost,
        @Size(max = 50) String supplier) {
}
