package com.grocerystore.backend.repository;

import com.grocerystore.backend.model.BusinessCustomer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessCustomerRepository
        extends JpaRepository<BusinessCustomer, Integer> {
}
