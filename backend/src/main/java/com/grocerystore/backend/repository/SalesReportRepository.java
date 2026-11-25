
package com.grocerystore.backend.repository;

import com.grocerystore.backend.model.Product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SalesReportRepository
        extends JpaRepository<Product, Integer> {

    // 1. Aggregate product sales and gross profit
    @Query(value = """
        SELECT
            Product.product_id,
            Product.product_name,
            COALESCE(SUM(OrderItem.quantity), 0),
            COALESCE(SUM(
                OrderItem.quantity * OrderItem.sale_price
            ), 0),
            CASE
                WHEN SUM(
                    CASE
                        WHEN OrderItem.order_id IS NOT NULL
                             AND OrderItem.unit_cost IS NULL
                        THEN 1 ELSE 0
                    END
                ) > 0
                THEN NULL
                ELSE COALESCE(SUM(
                    OrderItem.quantity *
                    (OrderItem.sale_price - OrderItem.unit_cost)
                ), 0)
            END,
            SUM(
                CASE
                    WHEN OrderItem.order_id IS NOT NULL
                         AND OrderItem.unit_cost IS NULL
                    THEN 1 ELSE 0
                END
            )
        FROM Product
        LEFT JOIN OrderItem
            ON Product.product_id = OrderItem.product_id
        GROUP BY Product.product_id
        ORDER BY 4 DESC
        """, nativeQuery = true)
    List<Object[]> getProductSales();

    // 2. Rank product categories by sales
    @Query(value = """
        SELECT
            Category.category_id,
            Category.category_name,
            COALESCE(SUM(OrderItem.quantity), 0),
            COALESCE(SUM(
                OrderItem.quantity * OrderItem.sale_price
            ), 0)
        FROM Category
        LEFT JOIN Product
            ON Category.category_id = Product.category_id
        LEFT JOIN OrderItem
            ON Product.product_id = OrderItem.product_id
        GROUP BY Category.category_id
        ORDER BY 4 DESC
        """, nativeQuery = true)
    List<Object[]> getCategorySales();

    // 3. Compare regions by sales volume and revenue
    @Query(value = """
        SELECT
            Region.region_id,
            Region.region_name,
            COUNT(DISTINCT Orders.order_id),
            COALESCE(SUM(OrderItem.quantity), 0),
            COALESCE(SUM(
                OrderItem.quantity * OrderItem.sale_price
            ), 0)
        FROM Region
        LEFT JOIN Store
            ON Region.region_id = Store.region_id
        LEFT JOIN Orders
            ON Store.store_id = Orders.store_id
        LEFT JOIN OrderItem
            ON Orders.order_id = OrderItem.order_id
        GROUP BY Region.region_id
        ORDER BY 4 DESC
        """, nativeQuery = true)
    List<Object[]> getRegionSales();

    // 4. Businesses purchasing a specified product
    @Query(value = """
        SELECT
            BusinessCustomer.customer_id,
            Customer.name,
            Product.product_id,
            Product.product_name,
            SUM(OrderItem.quantity),
            SUM(
                OrderItem.quantity * OrderItem.sale_price
            )
        FROM BusinessCustomer
        JOIN Customer
            ON BusinessCustomer.customer_id = Customer.customer_id
        JOIN Orders
            ON Customer.customer_id = Orders.customer_id
        JOIN OrderItem
            ON Orders.order_id = OrderItem.order_id
        JOIN Product
            ON OrderItem.product_id = Product.product_id
        WHERE Product.product_id = :productId
        GROUP BY
            BusinessCustomer.customer_id,
            Product.product_id
        ORDER BY 5 DESC, 6 DESC
        """, nativeQuery = true)
    List<Object[]> getBusinessPurchases(
            @Param("productId") Integer productId
    );
}
