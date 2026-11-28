package com.grocerystore.backend.service;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grocerystore.backend.dto.CategoryRequest;
import com.grocerystore.backend.dto.CategoryResponse;
import com.grocerystore.backend.exception.BadRequestException;
import com.grocerystore.backend.exception.ResourceNotFoundException;
import com.grocerystore.backend.util.Ids;
import com.grocerystore.backend.util.Text;

@Service
public class CategoryAdminService {

    private final JdbcTemplate jdbcTemplate;

    public CategoryAdminService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        requireUniqueName(request.categoryName(), null);
        int categoryId = Ids.next(jdbcTemplate, "Category", "category_id");
        jdbcTemplate.update(
                "INSERT INTO Category (category_id, category_name, description) VALUES (?, ?, ?)",
                categoryId,
                request.categoryName().trim(),
                Text.trimToNull(request.description()));
        return find(categoryId);
    }

    @Transactional
    public CategoryResponse update(Integer categoryId, CategoryRequest request) {
        find(categoryId);
        requireUniqueName(request.categoryName(), categoryId);
        jdbcTemplate.update(
                "UPDATE Category SET category_name = ?, description = ? WHERE category_id = ?",
                request.categoryName().trim(),
                Text.trimToNull(request.description()),
                categoryId);
        return find(categoryId);
    }

    private CategoryResponse find(Integer categoryId) {
        List<CategoryResponse> categories = jdbcTemplate.query(
                "SELECT category_id, category_name, description FROM Category WHERE category_id = ?",
                (resultSet, rowNumber) -> new CategoryResponse(
                        resultSet.getInt("category_id"),
                        resultSet.getString("category_name"),
                        resultSet.getString("description")),
                categoryId);
        if (categories.isEmpty()) {
            throw new ResourceNotFoundException("Category " + categoryId + " was not found");
        }
        return categories.getFirst();
    }

    private void requireUniqueName(String name, Integer excludeCategoryId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM Category
                WHERE LOWER(category_name) = LOWER(?) AND category_id <> ?
                """,
                Integer.class,
                name.trim(),
                excludeCategoryId == null ? 0 : excludeCategoryId);
        if (count != null && count > 0) {
            throw new BadRequestException("A category named " + name.trim() + " already exists");
        }
    }
}
