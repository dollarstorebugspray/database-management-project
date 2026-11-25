package com.grocerystore.backend.repository;

import com.grocerystore.backend.model.Customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository
        extends JpaRepository<Customer, Integer> {

    @Query(
        value = "SELECT COUNT(*) FROM Orders WHERE customer_id = :customerId",
        nativeQuery = true
    )
    long countOrdersByCustomerId(
            @Param("customerId") Integer customerId
    );
}
