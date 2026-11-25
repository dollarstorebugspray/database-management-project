package com.grocerystore.backend.controller;

import com.grocerystore.backend.model.Customer;
import com.grocerystore.backend.dto.CreateCustomerRequest;
import com.grocerystore.backend.repository.CustomerRepository;
import com.grocerystore.backend.service.CustomerService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.grocerystore.backend.repository.HomeCustomerRepository;
import com.grocerystore.backend.repository.BusinessCustomerRepository;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final HomeCustomerRepository homeCustomerRepository;
    private final BusinessCustomerRepository businessCustomerRepository;

    public CustomerController(
            CustomerRepository customerRepository,
            CustomerService customerService,
            HomeCustomerRepository homeCustomerRepository,
            BusinessCustomerRepository businessCustomerRepository) {

        this.customerRepository = customerRepository;
        this.customerService = customerService;
        this.homeCustomerRepository = homeCustomerRepository;
        this.businessCustomerRepository = businessCustomerRepository;
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


    @GetMapping("/{customerId}/details")
    public Map<String, Object> getCustomerDetails(
            @PathVariable Integer customerId) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Customer not found"
                ));

        Map<String, Object> details = new LinkedHashMap<>();

        details.put("customerId", customer.getCustomerId());
        details.put("name", customer.getName());
        details.put("street", customer.getStreet());
        details.put("city", customer.getCity());
        details.put("state", customer.getState());
        details.put("zipCode", customer.getZipCode());

        String type = customer.getCustomerType();

        String normalizedType =
                "home".equalsIgnoreCase(type) ? "Home" :
                "business".equalsIgnoreCase(type) ? "Business" : type;

        details.put("customerType", normalizedType);

        details.put("home", "Home".equals(normalizedType)
                ? homeCustomerRepository.findById(customerId).orElse(null)
                : null);

        details.put("business", "Business".equals(normalizedType)
                ? businessCustomerRepository.findById(customerId).orElse(null)
                : null);

        return details;
    }


    // Create a new customer
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Customer createCustomer(
            @RequestBody CreateCustomerRequest request) {

        return customerService.createCustomer(request);
    }

    // Update an existing customer
    @PutMapping("/{customerId}")
    public Customer updateCustomer(
            @PathVariable Integer customerId,
            @RequestBody CreateCustomerRequest request) {

        return customerService.updateCustomer(customerId, request);
    }


    // Delete a customer
    @DeleteMapping("/{customerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCustomer(
            @PathVariable Integer customerId) {

        customerService.deleteCustomer(customerId);
    }


}
