package com.grocerystore.backend.service;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grocerystore.backend.dto.ProductResponse;
import com.grocerystore.backend.dto.ProductSearch;
import com.grocerystore.backend.dto.StoreAvailability;
import com.grocerystore.backend.exception.BadRequestException;
import com.grocerystore.backend.exception.ResourceNotFoundException;
import com.grocerystore.backend.util.Text;

@Service
public class ProductService {

    static final int POPULAR_WINDOW_DAYS = 90;

    private static final Map<String, String> SORT_ORDERS = Map.of(
            "name", "p.product_name",
            "price_asc", "p.price ASC, p.product_name",
            "price_desc", "p.price DESC, p.product_name",
            "popular", "units_sold DESC, p.product_name");

    private static final String SELECT_PRODUCTS = """
            SELECT p.product_id, p.product_name, p.brand, p.price,
                   p.category_id, c.category_name, i.quantity,
                   COALESCE(sold.units, 0) AS units_sold
            FROM Product p
            JOIN Category c ON c.category_id = p.category_id
            JOIN Inventory i ON i.product_id = p.product_id AND i.store_id = ?
            LEFT JOIN (
                SELECT oi.product_id, SUM(oi.quantity) AS units
                FROM OrderItem oi
                JOIN Orders o ON o.order_id = oi.order_id
                WHERE o.store_id = ?
                  AND o.order_status <> 'CANCELLED'
                  AND o.order_date >= ?
                GROUP BY oi.product_id
            ) sold ON sold.product_id = p.product_id
            WHERE 1 = 1
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ProductImages images;

    public ProductService(JdbcTemplate jdbcTemplate, ProductImages images) {
        this.jdbcTemplate = jdbcTemplate;
        this.images = images;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> search(ProductSearch criteria) {
        String sort = normalizeSort(criteria.sort());
        if (criteria.minPrice() != null && criteria.maxPrice() != null
                && criteria.minPrice().compareTo(criteria.maxPrice()) > 0) {
            throw new BadRequestException("minPrice cannot be greater than maxPrice");
        }

        StringBuilder sql = new StringBuilder(SELECT_PRODUCTS);
        List<Object> args = baseArguments(criteria.storeId());

        // Search products by name
        String text = Text.trimToEmpty(criteria.search());
        if (!text.isEmpty()) {
            sql.append("""
                     AND (p.product_name LIKE ?
                          OR COALESCE(p.brand, '') LIKE ?
                          OR c.category_name LIKE ?)
                    """);
            String pattern = Text.likePattern(text);
            args.add(pattern);
            args.add(pattern);
            args.add(pattern);
        }
        // Get products by category
        if (criteria.categoryId() != null) {
            sql.append(" AND p.category_id = ?");
            args.add(criteria.categoryId());
        }
        if (criteria.minPrice() != null) {
            sql.append(" AND p.price >= ?");
            args.add(criteria.minPrice());
        }
        if (criteria.maxPrice() != null) {
            sql.append(" AND p.price <= ?");
            args.add(criteria.maxPrice());
        }
        if (criteria.inStockOnly()) {
            sql.append(" AND i.quantity > 0");
        }
        sql.append(" ORDER BY ").append(SORT_ORDERS.get(sort));
        if (criteria.limit() != null) {
            sql.append(" LIMIT ?");
            args.add(criteria.limit());
        }

        return jdbcTemplate.query(sql.toString(), this::mapProduct, args.toArray());
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Integer storeId, Integer productId) {
        List<Object> args = baseArguments(storeId);
        args.add(productId);
        List<ProductResponse> products = jdbcTemplate.query(
                SELECT_PRODUCTS + " AND p.product_id = ?",
                this::mapProduct,
                args.toArray());
        if (products.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Product " + productId + " is not available at store " + storeId);
        }
        return products.getFirst();
    }

    @Transactional(readOnly = true)
    public List<StoreAvailability> availability(Integer productId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM Product WHERE product_id = ?",
                Integer.class,
                productId);
        if (count == null || count == 0) {
            throw new ResourceNotFoundException("Product " + productId + " was not found");
        }
        return jdbcTemplate.query("""
                SELECT s.store_id, s.street, s.city, s.state, i.quantity
                FROM Inventory i
                JOIN Store s ON s.store_id = i.store_id
                WHERE i.product_id = ?
                ORDER BY s.store_id
                """,
                (resultSet, rowNumber) -> new StoreAvailability(
                        resultSet.getInt("store_id"),
                        resultSet.getString("street"),
                        resultSet.getString("city"),
                        resultSet.getString("state"),
                        resultSet.getInt("quantity")),
                productId);
    }

    static String normalizeSort(String sort) {
        String normalized = Text.trimToEmpty(sort).toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return "name";
        }
        if (!SORT_ORDERS.containsKey(normalized)) {
            throw new BadRequestException("sort must be one of name, price_asc, price_desc, popular");
        }
        return normalized;
    }

    private List<Object> baseArguments(Integer storeId) {
        List<Object> args = new ArrayList<>();
        args.add(storeId);
        args.add(storeId);
        args.add(Date.valueOf(LocalDate.now().minusDays(POPULAR_WINDOW_DAYS)));
        return args;
    }

    private ProductResponse mapProduct(ResultSet resultSet, int rowNumber) throws SQLException {
        int productId = resultSet.getInt("product_id");
        return new ProductResponse(
                productId,
                resultSet.getString("product_name"),
                resultSet.getString("brand"),
                resultSet.getBigDecimal("price"),
                resultSet.getInt("category_id"),
                resultSet.getString("category_name"),
                resultSet.getInt("quantity"),
                images.urlFor(productId));
    }
}
