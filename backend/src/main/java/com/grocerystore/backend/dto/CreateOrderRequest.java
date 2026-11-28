package com.grocerystore.backend.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @NotNull(message = "customerId is required")
        @Min(value = 1, message = "customerId must be at least 1")
        Integer customerId,

        @NotNull(message = "storeId is required")
        @Min(value = 1, message = "storeId must be at least 1")
        Integer storeId,

        @NotBlank(message = "paymentMethod is required")
        @Size(max = 30, message = "paymentMethod must be 30 characters or fewer")
        String paymentMethod,

        @NotEmpty(message = "items must contain at least one product")
        List<@Valid OrderLineRequest> items) {
}
