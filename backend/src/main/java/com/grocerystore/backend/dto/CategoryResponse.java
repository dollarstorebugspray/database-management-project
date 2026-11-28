package com.grocerystore.backend.dto;

import com.grocerystore.backend.model.Category;

public record CategoryResponse(
        Integer categoryId,
        String categoryName,
        String description) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getCategoryId(),
                category.getCategoryName(),
                category.getDescription());
    }
}
