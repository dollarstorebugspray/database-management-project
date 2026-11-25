package com.grocerystore.backend.repository;

import com.grocerystore.backend.model.Product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SalesReportRepository
        extends JpaRepository<Product, Integer> {

    @Query(value = """
        SELECT
            Product.product_id AS productId,
            Product.product_name AS productName,
            COALESCE(SUM(OrderItem.quantity), 0) AS totalQuantitySold,
            COALESCE(SUM(
                OrderItem.quantity * OrderItem.sale_price
            ), 0) AS totalRevenue
        FROM Product
        LEFT JOIN OrderItem
            ON Product.product_id = OrderItem.product_id
        GROUP BY Product.product_id
        ORDER BY totalRevenue DESC
        """, nativeQuery = true)
    List<Object[]> getProductSales();
}
