package com.grocerystore.backend.dto;

import com.grocerystore.backend.model.SessionUser;

public record AuthResponse(
        Integer accountId,
        String email,
        String role,
        Integer customerId,
        Integer salespersonId,
        String name,
        Integer storeId) {

    public static AuthResponse from(SessionUser user) {
        return new AuthResponse(
                user.accountId(),
                user.email(),
                user.role(),
                user.customerId(),
                user.salespersonId(),
                user.displayName(),
                user.storeId());
    }
}
