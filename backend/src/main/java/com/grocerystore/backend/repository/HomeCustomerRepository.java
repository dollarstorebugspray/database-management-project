package com.grocerystore.backend.repository;

import com.grocerystore.backend.model.HomeCustomer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HomeCustomerRepository
        extends JpaRepository<HomeCustomer, Integer> {
}
