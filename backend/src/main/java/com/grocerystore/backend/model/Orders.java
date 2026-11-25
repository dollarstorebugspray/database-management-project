package com.grocerystore.backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "Orders")
public class Orders {

    @Id
    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "salesperson_id")
    private Integer salespersonId;

    @Column(name = "store_id")
    private Integer storeId;

    public Orders() {
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public Integer getSalespersonId() {
        return salespersonId;
    }

    public void setSalespersonId(Integer salespersonId) {
        this.salespersonId = salespersonId;
    }

    public Integer getStoreId() {
        return storeId;
    }

    public void setStoreId(Integer storeId) {
        this.storeId = storeId;
    }
}
