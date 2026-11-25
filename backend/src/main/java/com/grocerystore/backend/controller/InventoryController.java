package com.grocerystore.backend.controller;

import com.grocerystore.backend.model.Inventory;
import com.grocerystore.backend.model.InventoryId;
import com.grocerystore.backend.repository.InventoryRepository;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryRepository inventoryRepository;

    public InventoryController(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    // Get all inventory records
    @GetMapping
    public List<Inventory> getAllInventory() {
        return inventoryRepository.findAll();
    }

    // Get inventory for a specific store
    @GetMapping("/store/{storeId}")
    public List<Inventory> getInventoryByStore(
            @PathVariable Integer storeId) {

        return inventoryRepository.findByStoreId(storeId);
    }

    // Get inventory for a specific product
    @GetMapping("/product/{productId}")
    public List<Inventory> getInventoryByProduct(
            @PathVariable Integer productId) {

        return inventoryRepository.findByProductId(productId);
    }

    // Get one inventory record using store ID and product ID
    @GetMapping("/store/{storeId}/product/{productId}")
    public Inventory getInventoryByStoreAndProduct(
            @PathVariable Integer storeId,
            @PathVariable Integer productId) {

        InventoryId id = new InventoryId(storeId, productId);

        return inventoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Inventory record not found"
                ));
    }

    // Update the inventory quantity for a product at a store
    @PatchMapping("/store/{storeId}/product/{productId}")
    public Inventory updateInventory(
            @PathVariable Integer storeId,
            @PathVariable Integer productId,
            @RequestBody UpdateInventoryRequest request) {

        if (request == null || request.quantity() == null
                || request.quantity() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Quantity must be zero or greater"
            );
        }

        InventoryId id = new InventoryId(storeId, productId);

        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Inventory record not found"
                ));

        inventory.setQuantity(request.quantity());

        return inventoryRepository.save(inventory);
    }

    public record UpdateInventoryRequest(Integer quantity) {
    }

}
