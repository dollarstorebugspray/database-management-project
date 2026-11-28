package com.grocerystore.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CustomerResponse(
        Integer customerId,
        String name,
        String customerType,
        String street,
        String city,
        String state,
        String zipCode,
        String maritalStatus,
        String gender,
        Integer age,
        BigDecimal income,
        String businessCategory,
        BigDecimal annualIncome,
        Integer orderCount,
        BigDecimal totalSpent,
        LocalDate lastOrderDate,
        boolean hasAccount) {
}
