package com.grocerystore.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Pattern(regexp = "(?i)HOME|BUSINESS") String customerType,
        @Size(max = 50) String street,
        @Size(max = 25) String city,
        @Size(max = 15) String state,
        @Size(max = 10) String zipCode,
        @Size(max = 10) String maritalStatus,
        @Size(max = 20) String gender,
        @Min(0) @Max(130) Integer age,
        @DecimalMin("0.0") BigDecimal income,
        @Size(max = 50) String businessCategory,
        @DecimalMin("0.0") BigDecimal annualIncome) {
}
