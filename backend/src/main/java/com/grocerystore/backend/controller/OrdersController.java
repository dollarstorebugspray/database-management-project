package com.grocerystore.backend.controller;

import com.grocerystore.backend.dto.CreateOrderRequest;
import com.grocerystore.backend.model.Orders;
import com.grocerystore.backend.repository.OrdersRepository;
import com.grocerystore.backend.service.OrderService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrdersController {

    private final OrdersRepository ordersRepository;
    private final OrderService orderService;

    public OrdersController(
            OrdersRepository ordersRepository,
            OrderService orderService) {

        this.ordersRepository = ordersRepository;
        this.orderService = orderService;
    }

    // Retrieve all orders
    @GetMapping
    public List<Orders> getAllOrders() {
        return ordersRepository.findAll();
    }

    // Retrieve an order by ID
    @GetMapping("/{orderId}")
    public Orders getOrderById(@PathVariable Integer orderId) {

        return ordersRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found"
                ));
    }

    // Retrieve orders belonging to a customer
    @GetMapping("/customer/{customerId}")
    public List<Orders> getOrdersByCustomerId(
            @PathVariable Integer customerId) {

        return ordersRepository.findByCustomerId(customerId);
    }

    // Retrieve orders belonging to a store
    @GetMapping("/store/{storeId}")
    public List<Orders> getOrdersByStoreId(
            @PathVariable Integer storeId) {

        return ordersRepository.findByStoreId(storeId);
    }

    // Create a new order
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Orders createOrder(@RequestBody CreateOrderRequest request) {

        return orderService.createOrder(request);
    }
}
