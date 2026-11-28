package com.grocerystore.backend.controller;

import java.util.List;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.grocerystore.backend.dto.CustomerRequest;
import com.grocerystore.backend.dto.CustomerResponse;
import com.grocerystore.backend.dto.OrderResponse;
import com.grocerystore.backend.exception.ForbiddenException;
import com.grocerystore.backend.model.SessionUser;
import com.grocerystore.backend.service.AuthService;
import com.grocerystore.backend.service.CustomerService;
import com.grocerystore.backend.service.OrderService;

@Validated
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final OrderService orderService;
    private final AuthService authService;

    public CustomerController(
            CustomerService customerService,
            OrderService orderService,
            AuthService authService) {
        this.customerService = customerService;
        this.orderService = orderService;
        this.authService = authService;
    }

    // Retrieve all customers
    @GetMapping
    public List<CustomerResponse> getCustomers(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) String type,
            HttpSession session) {
        authService.requireStaff(session);
        return customerService.findAll(search, type);
    }

    // Retrieve a specific customer by ID
    @GetMapping("/{customerId}")
    public CustomerResponse getCustomer(
            @PathVariable @Min(1) Integer customerId,
            HttpSession session) {
        authService.requireCustomerAccess(customerId, session);
        return customerService.findById(customerId);
    }

    // Create a new customer
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse createCustomer(
            @Valid @RequestBody CustomerRequest request,
            HttpSession session) {
        authService.requireStaff(session);
        return customerService.create(request);
    }

    // Update an existing customer
    @PutMapping("/{customerId}")
    public CustomerResponse updateCustomer(
            @PathVariable @Min(1) Integer customerId,
            @Valid @RequestBody CustomerRequest request,
            HttpSession session) {
        SessionUser user = authService.requireCustomerAccess(customerId, session);
        // Do not allow changing customer types
        if (!user.isStaff()) {
            CustomerResponse current = customerService.findById(customerId);
            if (!current.customerType().equalsIgnoreCase(request.customerType().trim())) {
                throw new ForbiddenException("Please ask a staff member to change your customer type");
            }
        }
        CustomerResponse updated = customerService.update(customerId, request);
        authService.refreshDisplayName(session, customerId, updated.name());
        return updated;
    }

    // Retrieve orders belonging to a customer
    @GetMapping("/{customerId}/orders")
    public List<OrderResponse> getCustomerOrders(
            @PathVariable @Min(1) Integer customerId,
            HttpSession session) {
        authService.requireCustomerAccess(customerId, session);
        return orderService.findByCustomerId(customerId);
    }

    // Delete a customer
    @DeleteMapping("/{customerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCustomer(
            @PathVariable @Min(1) Integer customerId,
            HttpSession session) {
        authService.requireStaff(session);
        customerService.delete(customerId);
    }
}
