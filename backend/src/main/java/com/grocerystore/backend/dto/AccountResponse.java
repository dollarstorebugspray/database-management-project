package com.grocerystore.backend.dto;

import java.time.LocalDateTime;

public record AccountResponse(
        Integer accountId,
        String email,
        String role,
        Integer customerId,
        Integer salespersonId,
        String name,
        LocalDateTime createdAt) {
}
