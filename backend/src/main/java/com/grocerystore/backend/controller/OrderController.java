package com.grocerystore.backend.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.grocerystore.backend.dto.CreateOrderRequest;
import com.grocerystore.backend.dto.ItemChangeRequest;
import com.grocerystore.backend.dto.ItemChangeResponse;
import com.grocerystore.backend.dto.OrderResponse;
import com.grocerystore.backend.model.SessionUser;
import com.grocerystore.backend.service.AuthService;
import com.grocerystore.backend.service.OrderService;

@Validated
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final AuthService authService;

    public OrderController(OrderService orderService, AuthService authService) {
        this.orderService = orderService;
        this.authService = authService;
    }

    // Create a new order
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            HttpSession session) {
        SessionUser user = authService.requireCustomerAccess(request.customerId(), session);
        Integer actingSalespersonId = user.isStaff() ? user.salespersonId() : null;
        return orderService.create(request, actingSalespersonId);
    }

    // Retrieve an order by ID
    @GetMapping("/{orderId}")
    public OrderResponse getOrder(
            @PathVariable @Min(1) Integer orderId,
            HttpSession session) {
        authService.requireUser(session);
        OrderResponse order = orderService.findById(orderId);
        authService.requireCustomerAccess(order.customerId(), session);
        return order;
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(
            @PathVariable @Min(1) Integer orderId,
            HttpSession session) {
        authService.requireUser(session);
        OrderResponse order = orderService.findById(orderId);
        authService.requireCustomerAccess(order.customerId(), session);
        return orderService.cancelByCustomer(orderId);
    }

    @PostMapping("/{orderId}/cancel-items")
    public ItemChangeResponse cancelItems(
            @PathVariable @Min(1) Integer orderId,
            @Valid @RequestBody ItemChangeRequest request,
            HttpSession session) {
        authService.requireUser(session);
        OrderResponse order = orderService.findById(orderId);
        SessionUser user = authService.requireCustomerAccess(order.customerId(), session);
        return orderService.cancelItems(orderId, request.items(), user.isStaff());
    }
}
