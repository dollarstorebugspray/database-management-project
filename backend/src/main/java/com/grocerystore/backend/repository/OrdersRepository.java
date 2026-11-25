package com.grocerystore.backend.repository;

import com.grocerystore.backend.model.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrdersRepository
        extends JpaRepository<Orders, Integer> {

    List<Orders> findByCustomerId(Integer customerId);

    List<Orders> findByStoreId(Integer storeId);
}
