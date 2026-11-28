package com.grocerystore.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StoreRequest(
        @NotBlank @Size(max = 50) String street,
        @NotBlank @Size(max = 25) String city,
        @NotBlank @Size(max = 15) String state,
        @NotBlank @Size(max = 10)
        @Pattern(regexp = "\\d{5}(-\\d{4})?", message = "must be a 5-digit ZIP code")
        String zipCode,
        @Size(max = 50) String manager,
        @NotNull @Min(1) Integer regionId) {
}
