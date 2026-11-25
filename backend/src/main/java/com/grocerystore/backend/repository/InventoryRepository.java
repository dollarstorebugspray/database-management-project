package com.grocerystore.backend.repository;

import com.grocerystore.backend.model.Inventory;
import com.grocerystore.backend.model.InventoryId;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InventoryRepository
        extends JpaRepository<Inventory, InventoryId> {

    List<Inventory> findByStoreId(Integer storeId);

    List<Inventory> findByProductId(Integer productId);
}
