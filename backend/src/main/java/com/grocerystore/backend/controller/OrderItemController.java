package com.grocerystore.backend.controller;

import com.grocerystore.backend.model.OrderItem;
import com.grocerystore.backend.repository.OrderItemRepository;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order-items")
public class OrderItemController {

    private final OrderItemRepository orderItemRepository;

    public OrderItemController(
            OrderItemRepository orderItemRepository) {

        this.orderItemRepository = orderItemRepository;
    }

    // Retrieve all products belonging to a specific order
    @GetMapping("/order/{orderId}")
    public List<OrderItem> getOrderItems(
            @PathVariable Integer orderId) {

        return orderItemRepository.findByOrderId(orderId);
    }
}
