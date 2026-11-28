package com.grocerystore.backend.dto;

import java.math.BigDecimal;

public record SalespersonResponse(
        Integer salespersonId,
        String name,
        String street,
        String city,
        String state,
        String zipCode,
        String email,
        String jobTitle,
        BigDecimal salary,
        Integer storeId,
        String storeCity,
        String accountEmail) {

    public SalespersonResponse withoutSalary() {
        return new SalespersonResponse(
                salespersonId, name, street, city, state, zipCode, email,
                jobTitle, null, storeId, storeCity, accountEmail);
    }
}
