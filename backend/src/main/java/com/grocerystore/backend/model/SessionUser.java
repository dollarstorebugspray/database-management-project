package com.grocerystore.backend.model;

import java.io.Serializable;

public record SessionUser(
        Integer accountId,
        String email,
        String role,
        Integer customerId,
        Integer salespersonId,
        String displayName,
        Integer storeId) implements Serializable {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public boolean isStaff() {
        return "ADMIN".equals(role) || "EMPLOYEE".equals(role);
    }
}
