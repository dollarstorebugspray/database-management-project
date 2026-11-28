package com.grocerystore.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @Pattern(regexp = "(?i)HOME|BUSINESS", message = "must be HOME or BUSINESS") String customerType,
        @Size(max = 50) String businessCategory) {
}
