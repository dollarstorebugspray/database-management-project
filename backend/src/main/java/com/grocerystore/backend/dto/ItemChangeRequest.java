package com.grocerystore.backend.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record ItemChangeRequest(
        @NotEmpty(message = "items must contain at least one product")
        List<@Valid OrderLineRequest> items,
        Boolean restock) {
}
