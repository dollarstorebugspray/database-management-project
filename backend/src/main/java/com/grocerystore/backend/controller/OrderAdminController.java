package com.grocerystore.backend.controller;

import java.util.List;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.grocerystore.backend.dto.AssignSalespersonRequest;
import com.grocerystore.backend.dto.ItemChangeRequest;
import com.grocerystore.backend.dto.ItemChangeResponse;
import com.grocerystore.backend.dto.OrderResponse;
import com.grocerystore.backend.dto.OrderSearch;
import com.grocerystore.backend.dto.OrderStatusRequest;
import com.grocerystore.backend.service.AuthService;
import com.grocerystore.backend.service.OrderService;

@Validated
@RestController
@RequestMapping("/api/admin/orders")
public class OrderAdminController {

    private final OrderService orderService;
    private final AuthService authService;

    public OrderAdminController(OrderService orderService, AuthService authService) {
        this.orderService = orderService;
        this.authService = authService;
    }

    // Retrieve all orders
    @GetMapping
    public List<OrderResponse> getOrders(
            @RequestParam(required = false) @Min(1) Integer storeId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) Integer limit,
            HttpSession session) {
        authService.requireStaff(session);
        return orderService.search(new OrderSearch(storeId, status, search, limit));
    }

    @PutMapping("/{orderId}/status")
    public OrderResponse updateStatus(
            @PathVariable @Min(1) Integer orderId,
            @Valid @RequestBody OrderStatusRequest request,
            HttpSession session) {
        authService.requireStaff(session);
        return orderService.updateStatus(orderId, request.status());
    }

    @PutMapping("/{orderId}/salesperson")
    public OrderResponse assignSalesperson(
            @PathVariable @Min(1) Integer orderId,
            @Valid @RequestBody AssignSalespersonRequest request,
            HttpSession session) {
        authService.requireStaff(session);
        return orderService.assignSalesperson(orderId, request.salespersonId());
    }

    @PostMapping("/{orderId}/return-items")
    public ItemChangeResponse returnItems(
            @PathVariable @Min(1) Integer orderId,
            @Valid @RequestBody ItemChangeRequest request,
            HttpSession session) {
        authService.requireStaff(session);
        boolean restock = request.restock() == null || request.restock();
        return orderService.returnItems(orderId, request.items(), restock);
    }
}
