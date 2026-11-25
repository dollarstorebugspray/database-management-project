package com.grocerystore.backend.controller;

import com.grocerystore.backend.model.Product;
import com.grocerystore.backend.repository.ProductRepository;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // Get all products or filter by maximum price
    @GetMapping
    public List<Product> getAllProducts(
            @RequestParam(required = false) BigDecimal maxPrice) {

        if (maxPrice == null) {
            return productRepository.findAll();
        }

        if (maxPrice.signum() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Maximum price cannot be negative"
            );
        }

        return productRepository.findByPriceLessThanEqual(maxPrice);
    }

    // Search products by name
    @GetMapping("/search")
    public List<Product> searchProducts(@RequestParam String name) {

        return productRepository.findByProductNameContainingIgnoreCase(name);
    }

    // Get products by category
    @GetMapping("/category/{categoryId}")
    public List<Product> getProductsByCategory(
            @PathVariable Integer categoryId) {

        return productRepository.findByCategoryId(categoryId);
    }

    // Get a specific product by ID
    @GetMapping("/{productId}")
    public Product getProductById(
            @PathVariable Integer productId) {

        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found"
                ));
    }
}
