package com.grocerystore.backend.service;

import com.grocerystore.backend.dto.CreateOrderRequest;
import com.grocerystore.backend.model.*;
import com.grocerystore.backend.repository.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class OrderService {

    private final OrdersRepository ordersRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final EntityManager entityManager;

    public OrderService(
            OrdersRepository ordersRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            EntityManager entityManager) {

        this.ordersRepository = ordersRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public Orders createOrder(CreateOrderRequest request) {

        // Validate general order information
        if (request == null
                || request.orderId() == null
                || request.customerId() == null
                || request.storeId() == null
                || request.paymentMethod() == null
                || request.paymentMethod().isBlank()
                || request.paymentMethod().length() > 30
                || request.items() == null
                || request.items().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid order information"
            );
        }

        // Prevent duplicate order IDs
        if (ordersRepository.existsById(request.orderId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order ID already exists"
            );
        }

        // Verify the customer exists
        if (!customerRepository.existsById(request.customerId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Customer not found"
            );
        }

        // Verify the store exists
        if (!recordExists("Store", "store_id", request.storeId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Store not found"
            );
        }

        // Verify the salesperson, if provided
        if (request.salespersonId() != null) {

            if (!recordExists(
                    "Salesperson",
                    "salesperson_id",
                    request.salespersonId())) {

                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Salesperson not found"
                );
            }
        }

        // Make sure the order does not repeat any product
        Set<Integer> productIds = new HashSet<>();

        for (CreateOrderRequest.OrderItemRequest item : request.items()) {

            if (item == null
                    || item.productId() == null
                    || item.quantity() == null
                    || item.quantity() <= 0) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid order item"
                );
            }

            if (!productIds.add(item.productId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Duplicate product in order"
                );
            }
        }

        // Lock inventory records in a consistent order
        List<CreateOrderRequest.OrderItemRequest> sortedItems =
                request.items().stream()
                        .sorted(Comparator.comparing(
                                CreateOrderRequest.OrderItemRequest::productId))
                        .toList();

        for (CreateOrderRequest.OrderItemRequest item : sortedItems) {

            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Product not found: " + item.productId()
                    ));

            if (product.getPrice() == null
                    || product.getPrice().signum() < 0) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Invalid product price"
                );
            }

            InventoryId inventoryId = new InventoryId(
                    request.storeId(),
                    item.productId()
            );

            Inventory inventory = entityManager.find(
                    Inventory.class,
                    inventoryId,
                    LockModeType.PESSIMISTIC_WRITE
            );

            // Check whether the store carries the product
            if (inventory == null) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Product is not stocked at this store"
                );
            }

            // Prevent purchases that exceed available inventory
            if (inventory.getQuantity() < item.quantity()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Insufficient inventory for product "
                                + item.productId()
                );
            }

            // Reduce available stock
            inventory.setQuantity(
                    inventory.getQuantity() - item.quantity()
            );
        }

        // Create the general order record
        Orders order = new Orders();

        order.setOrderId(request.orderId());
        order.setOrderDate(LocalDate.now());
        order.setPaymentMethod(request.paymentMethod());
        order.setCustomerId(request.customerId());
        order.setSalespersonId(request.salespersonId());
        order.setStoreId(request.storeId());
	orderItem.setUnitCost(product.getCostPrice());

        ordersRepository.saveAndFlush(order);

        // Create an OrderItem for each purchased product
        for (CreateOrderRequest.OrderItemRequest item : sortedItems) {

            Product product = productRepository.findById(
                    item.productId()
            ).orElseThrow();

            OrderItem orderItem = new OrderItem();

            orderItem.setOrderId(request.orderId());
            orderItem.setProductId(item.productId());
            orderItem.setQuantity(item.quantity());
            orderItem.setSalePrice(product.getPrice());

            orderItemRepository.save(orderItem);
        }

        entityManager.flush();

        return order;
    }

    // Check whether a referenced database record exists
    private boolean recordExists(
            String table,
            String column,
            Integer id) {

        Object result = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM " + table
                        + " WHERE " + column + " = :id"
        )
        .setParameter("id", id)
        .getSingleResult();

        return ((Number) result).longValue() > 0;
    }
}
