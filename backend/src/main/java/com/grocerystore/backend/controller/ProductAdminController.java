package com.grocerystore.backend.controller;

import java.util.List;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.grocerystore.backend.dto.CategoryRequest;
import com.grocerystore.backend.dto.CategoryResponse;
import com.grocerystore.backend.dto.InventoryItemResponse;
import com.grocerystore.backend.dto.ProductResponse;
import com.grocerystore.backend.dto.ProductWriteRequest;
import com.grocerystore.backend.dto.PurchaseResponse;
import com.grocerystore.backend.dto.RestockRequest;
import com.grocerystore.backend.service.AuthService;
import com.grocerystore.backend.service.CategoryAdminService;
import com.grocerystore.backend.service.ProductAdminService;

@Validated
@RestController
public class ProductAdminController {

    private final ProductAdminService productAdminService;
    private final CategoryAdminService categoryAdminService;
    private final AuthService authService;

    public ProductAdminController(
            ProductAdminService productAdminService,
            CategoryAdminService categoryAdminService,
            AuthService authService) {
        this.productAdminService = productAdminService;
        this.categoryAdminService = categoryAdminService;
        this.authService = authService;
    }

    @PostMapping("/api/admin/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(
            @Valid @RequestBody ProductWriteRequest request,
            HttpSession session) {
        authService.requireStaff(session);
        return productAdminService.create(request);
    }

    @PutMapping("/api/admin/products/{productId}")
    public ProductResponse updateProduct(
            @PathVariable @Min(1) Integer productId,
            @Valid @RequestBody ProductWriteRequest request,
            HttpSession session) {
        authService.requireStaff(session);
        return productAdminService.update(productId, request);
    }

    // Get inventory for a specific store
    @GetMapping("/api/admin/inventory")
    public List<InventoryItemResponse> getInventory(
            @RequestParam(defaultValue = "1") @Min(1) Integer storeId,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "5") @Min(0) @Max(1000) Integer lowStockThreshold,
            @RequestParam(defaultValue = "false") boolean lowStockOnly,
            HttpSession session) {
        authService.requireStaff(session);
        return productAdminService.inventory(storeId, search, lowStockThreshold, lowStockOnly);
    }

    @PostMapping("/api/admin/inventory/restock")
    public ProductResponse restock(
            @Valid @RequestBody RestockRequest request,
            HttpSession session) {
        authService.requireStaff(session);
        return productAdminService.restock(request);
    }

    @GetMapping("/api/admin/purchases")
    public List<PurchaseResponse> getPurchases(
            @RequestParam(required = false) @Min(1) Integer storeId,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "200") @Min(1) @Max(1000) Integer limit,
            HttpSession session) {
        authService.requireStaff(session);
        return productAdminService.purchases(storeId, search, limit);
    }

    @PostMapping("/api/admin/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(
            @Valid @RequestBody CategoryRequest request,
            HttpSession session) {
        authService.requireStaff(session);
        return categoryAdminService.create(request);
    }

    @PutMapping("/api/admin/categories/{categoryId}")
    public CategoryResponse updateCategory(
            @PathVariable @Min(1) Integer categoryId,
            @Valid @RequestBody CategoryRequest request,
            HttpSession session) {
        authService.requireStaff(session);
        return categoryAdminService.update(categoryId, request);
    }
}
