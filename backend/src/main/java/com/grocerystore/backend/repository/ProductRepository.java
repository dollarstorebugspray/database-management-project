package com.grocerystore.backend.repository;

import com.grocerystore.backend.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface ProductRepository
        extends JpaRepository<Product, Integer> {

    List<Product> findByProductNameContainingIgnoreCase(String name);

    List<Product> findByCategoryId(Integer categoryId);

    List<Product> findByPriceLessThanEqual(BigDecimal maxPrice);
}
