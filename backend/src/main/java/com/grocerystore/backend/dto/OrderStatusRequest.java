package com.grocerystore.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OrderStatusRequest(
        @NotBlank
        @Pattern(regexp = "(?i)PENDING|PAID|PROCESSING|COMPLETED|CANCELLED")
        String status) {
}
