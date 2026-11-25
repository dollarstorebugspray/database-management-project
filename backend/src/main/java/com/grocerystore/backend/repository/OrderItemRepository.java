package com.grocerystore.backend.repository;

import com.grocerystore.backend.model.OrderItem;
import com.grocerystore.backend.model.OrderItemId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository
        extends JpaRepository<OrderItem, OrderItemId> {

    List<OrderItem> findByOrderId(Integer orderId);

    List<OrderItem> findByProductId(Integer productId);
}
