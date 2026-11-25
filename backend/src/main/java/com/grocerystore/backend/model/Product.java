
package com.grocerystore.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "Product")
public class Product {

    @Id
    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "brand")
    private String brand;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "cost_price")
    private BigDecimal costPrice;


    public Product() {
    }

    public Integer getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getBrand() {
        return brand;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

public BigDecimal getCostPrice() {
    return costPrice;
}

public void setCostPrice(BigDecimal costPrice) {
    this.costPrice = costPrice;
}

}
