package com.grocerystore.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OrderResponse(
        Integer orderId,
        LocalDate orderDate,
        Integer customerId,
        String customerName,
        String customerType,
        Integer storeId,
        String storeCity,
        Integer salespersonId,
        String salespersonName,
        String paymentMethod,
        String orderStatus,
        List<OrderItemResponse> items,
        Integer itemCount,
        BigDecimal total) {
}
