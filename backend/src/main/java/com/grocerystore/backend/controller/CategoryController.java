package com.grocerystore.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.grocerystore.backend.dto.CategoryResponse;
import com.grocerystore.backend.repository.CategoryRepository;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAllByOrderByCategoryNameAsc()
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }
}
