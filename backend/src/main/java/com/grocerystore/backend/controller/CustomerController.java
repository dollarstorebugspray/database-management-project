package com.grocerystore.backend.controller;

import com.grocerystore.backend.model.Customer;
import com.grocerystore.backend.dto.CreateCustomerRequest;
import com.grocerystore.backend.repository.CustomerRepository;
import com.grocerystore.backend.service.CustomerService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final CustomerService customerService;

    public CustomerController(
            CustomerRepository customerRepository,
            CustomerService customerService) {

        this.customerRepository = customerRepository;
        this.customerService = customerService;
    }

    // Retrieve all customers
    @GetMapping
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    // Retrieve a specific customer by ID
    @GetMapping("/{customerId}")
    public Customer getCustomerById(
            @PathVariable Integer customerId) {

        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Customer not found"
                ));
    }

    // Create a new customer
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Customer createCustomer(
            @RequestBody CreateCustomerRequest request) {

        return customerService.createCustomer(request);
    }
}
