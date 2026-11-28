package com.grocerystore.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.grocerystore.backend.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

    List<Category> findAllByOrderByCategoryNameAsc();
}
