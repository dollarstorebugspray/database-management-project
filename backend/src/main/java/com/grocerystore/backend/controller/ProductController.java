package com.grocerystore.backend.controller;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.grocerystore.backend.dto.ProductResponse;
import com.grocerystore.backend.dto.ProductSearch;
import com.grocerystore.backend.dto.StoreAvailability;
import com.grocerystore.backend.service.ProductService;

@Validated
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // Get all products or filter by maximum price
    @GetMapping
    public List<ProductResponse> getProducts(
            @RequestParam(defaultValue = "1") @Min(1) Integer storeId,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) @Min(1) Integer categoryId,
            @RequestParam(required = false) @DecimalMin("0.0") BigDecimal minPrice,
            @RequestParam(required = false) @DecimalMin("0.0") BigDecimal maxPrice,
            @RequestParam(defaultValue = "false") boolean inStock,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(required = false) @Min(1) @Max(200) Integer limit) {
        return productService.search(new ProductSearch(
                storeId, search, categoryId, minPrice, maxPrice, inStock, sort, limit));
    }

    @GetMapping("/popular")
    public List<ProductResponse> getPopularProducts(
            @RequestParam(defaultValue = "1") @Min(1) Integer storeId,
            @RequestParam(defaultValue = "8") @Min(1) @Max(50) Integer limit) {
        return productService.search(new ProductSearch(
                storeId, null, null, null, null, true, "popular", limit));
    }

    // Get a specific product by ID
    @GetMapping("/{productId}")
    public ProductResponse getProduct(
            @PathVariable @Min(1) Integer productId,
            @RequestParam(defaultValue = "1") @Min(1) Integer storeId) {
        return productService.findById(storeId, productId);
    }

    // Get inventory for a specific product
    @GetMapping("/{productId}/availability")
    public List<StoreAvailability> getAvailability(@PathVariable @Min(1) Integer productId) {
        return productService.availability(productId);
    }
}
