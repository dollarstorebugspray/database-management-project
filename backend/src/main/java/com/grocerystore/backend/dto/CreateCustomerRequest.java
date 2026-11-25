package com.grocerystore.backend.dto;

import java.math.BigDecimal;

public record CreateCustomerRequest(
        Integer customerId,
        String name,
        String street,
        String city,
        String state,
        String zipCode,
        String customerType,
        HomeDetails home,
        BusinessDetails business
) {
    public record HomeDetails(
            String maritalStatus,
            String gender,
            Integer age,
            BigDecimal income
    ) {
    }

    public record BusinessDetails(
            String businessCategory,
            BigDecimal annualIncome
    ) {
    }
}
