package com.grocerystore.backend.service;

import com.grocerystore.backend.dto.CreateCustomerRequest;
import com.grocerystore.backend.model.Customer;
import com.grocerystore.backend.model.HomeCustomer;
import com.grocerystore.backend.model.BusinessCustomer;

import com.grocerystore.backend.repository.CustomerRepository;
import com.grocerystore.backend.repository.HomeCustomerRepository;
import com.grocerystore.backend.repository.BusinessCustomerRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final HomeCustomerRepository homeCustomerRepository;
    private final BusinessCustomerRepository businessCustomerRepository;

    public CustomerService(
            CustomerRepository customerRepository,
            HomeCustomerRepository homeCustomerRepository,
            BusinessCustomerRepository businessCustomerRepository) {

        this.customerRepository = customerRepository;
        this.homeCustomerRepository = homeCustomerRepository;
        this.businessCustomerRepository = businessCustomerRepository;
    }

    @Transactional
    public Customer createCustomer(CreateCustomerRequest request) {

        // Validate general customer information
        if (request == null || request.customerId() == null
                || request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Customer ID and name are required"
            );
        }

        if (request.name().length() > 50) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Customer name cannot exceed 50 characters"
            );
        }

        // Prevent duplicate customer IDs
        if (customerRepository.existsById(request.customerId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Customer ID already exists"
            );
        }

        // Validate customer type and subtype information
        String type = request.customerType();

        if (!"Home".equals(type) && !"Business".equals(type)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Customer type must be Home or Business"
            );
        }

        if ("Home".equals(type)) {
            if (request.home() == null || request.business() != null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Home customer must have only home details"
                );
            }

            if (request.home().age() != null
                    && request.home().age() < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Age cannot be negative"
                );
            }

            if (request.home().income() != null
                    && request.home().income().signum() < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Income cannot be negative"
                );
            }
        }

        if ("Business".equals(type)) {
            if (request.business() == null || request.home() != null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Business customer must have only business details"
                );
            }

            if (request.business().annualIncome() != null
                    && request.business().annualIncome().signum() < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Annual income cannot be negative"
                );
            }
        }

        // Create the general customer
        Customer customer = new Customer();

        customer.setCustomerId(request.customerId());
        customer.setName(request.name());
        customer.setStreet(request.street());
        customer.setCity(request.city());
        customer.setState(request.state());
        customer.setZipCode(request.zipCode());
        customer.setCustomerType(type);

        customerRepository.saveAndFlush(customer);

        // Create the appropriate customer subtype
        if ("Home".equals(type)) {

            HomeCustomer home = new HomeCustomer();

            home.setCustomerId(request.customerId());
            home.setMaritalStatus(request.home().maritalStatus());
            home.setGender(request.home().gender());
            home.setAge(request.home().age());
            home.setIncome(request.home().income());

            homeCustomerRepository.save(home);

        } else {

            BusinessCustomer business = new BusinessCustomer();

            business.setCustomerId(request.customerId());
            business.setBusinessCategory(
                    request.business().businessCategory()
            );
            business.setAnnualIncome(
                    request.business().annualIncome()
            );

            businessCustomerRepository.save(business);
        }

        return customer;
    }
}
