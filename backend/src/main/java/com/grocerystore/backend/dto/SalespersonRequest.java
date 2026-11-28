package com.grocerystore.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SalespersonRequest(
        @NotBlank @Size(max = 50) String name,
        @Size(max = 50) String street,
        @Size(max = 25) String city,
        @Size(max = 15) String state,
        @Size(max = 10) String zipCode,
        @Email @Size(max = 50) String email,
        @NotBlank @Size(max = 50) String jobTitle,
        @DecimalMin("0.0") BigDecimal salary,
        @NotNull @Min(1) Integer storeId) {
}
