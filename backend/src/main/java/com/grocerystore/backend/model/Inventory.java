package com.grocerystore.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "Inventory")
@IdClass(InventoryId.class)
public class Inventory {

    @Id
    @Column(name = "store_id")
    private Integer storeId;

    @Id
    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "quantity")
    private Integer quantity;

    public Inventory() {
    }

    public Integer getStoreId() {
        return storeId;
    }

    public void setStoreId(Integer storeId) {
        this.storeId = storeId;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
