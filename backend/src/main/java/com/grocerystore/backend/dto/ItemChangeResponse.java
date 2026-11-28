package com.grocerystore.backend.dto;

import java.math.BigDecimal;

public record ItemChangeResponse(
        OrderResponse order,
        BigDecimal refund,
        int unitsRemoved,
        boolean orderCancelled) {
}
