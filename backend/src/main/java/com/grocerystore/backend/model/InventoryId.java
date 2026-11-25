package com.grocerystore.backend.model;

import java.io.Serializable;
import java.util.Objects;

public class InventoryId implements Serializable {

    private Integer storeId;
    private Integer productId;

    public InventoryId() {
    }

    public InventoryId(Integer storeId, Integer productId) {
        this.storeId = storeId;
        this.productId = productId;
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof InventoryId)) return false;

        InventoryId other = (InventoryId) obj;

        return Objects.equals(storeId, other.storeId)
                && Objects.equals(productId, other.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storeId, productId);
    }
}
