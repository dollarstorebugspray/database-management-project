package com.grocerystore.backend.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grocerystore.backend.dto.InventoryItemResponse;
import com.grocerystore.backend.dto.ProductResponse;
import com.grocerystore.backend.dto.ProductWriteRequest;
import com.grocerystore.backend.dto.PurchaseResponse;
import com.grocerystore.backend.dto.RestockRequest;
import com.grocerystore.backend.exception.BadRequestException;
import com.grocerystore.backend.exception.ResourceNotFoundException;
import com.grocerystore.backend.util.Ids;
import com.grocerystore.backend.util.Text;

@Service
public class ProductAdminService {

    private final JdbcTemplate jdbcTemplate;
    private final ProductImages images;

    public ProductAdminService(JdbcTemplate jdbcTemplate, ProductImages images) {
        this.jdbcTemplate = jdbcTemplate;
        this.images = images;
    }

    @Transactional
    public ProductResponse create(ProductWriteRequest request) {
        if (request.quantity() > 0 && request.unitCost() == null) {
            throw new BadRequestException("unitCost is required when the product starts with stock");
        }
        requireCategory(request.categoryId());
        requireStore(request.storeId());
        requireUniqueProduct(request.productName(), request.brand(), null);

        int productId = Ids.next(jdbcTemplate, "Product", "product_id");

        jdbcTemplate.update("""
                INSERT INTO Product
                    (product_id, product_name, brand, price, cost_price, category_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                productId,
                request.productName().trim(),
                Text.trimToNull(request.brand()),
                request.price(),
                request.unitCost(),
                request.categoryId());

        jdbcTemplate.update("""
                INSERT INTO Inventory (store_id, product_id, quantity)
                VALUES (?, ?, ?)
                """,
                request.storeId(),
                productId,
                request.quantity());
        if (request.quantity() > 0) {
            recordPurchase(request.storeId(), productId, request.quantity(), request.unitCost(), request.supplier());
        }

        return findResponse(request.storeId(), productId);
    }

    @Transactional
    public ProductResponse update(Integer productId, ProductWriteRequest request) {
        requireProduct(productId);
        requireCategory(request.categoryId());
        requireStore(request.storeId());
        requireUniqueProduct(request.productName(), request.brand(), productId);

        jdbcTemplate.update("""
                UPDATE Product
                SET product_name = ?, brand = ?, price = ?, category_id = ?
                WHERE product_id = ?
                """,
                request.productName().trim(),
                Text.trimToNull(request.brand()),
                request.price(),
                request.categoryId(),
                productId);

        // Update the inventory quantity for a product at a store
        jdbcTemplate.update("""
                INSERT INTO Inventory (store_id, product_id, quantity)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE quantity = VALUES(quantity)
                """,
                request.storeId(),
                productId,
                request.quantity());

        return findResponse(request.storeId(), productId);
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponse> inventory(
            Integer storeId,
            String search,
            int lowStockThreshold,
            boolean lowStockOnly) {
        requireStore(storeId);
        StringBuilder sql = new StringBuilder("""
                SELECT p.product_id, p.product_name, p.brand, p.price,
                       p.category_id, c.category_name, i.quantity,
                       COALESCE(
                           (SELECT pu.unit_cost FROM Purchase pu
                            WHERE pu.product_id = p.product_id AND pu.store_id = ?
                            ORDER BY pu.purchase_date DESC, pu.purchase_id DESC LIMIT 1),
                           (SELECT pu.unit_cost FROM Purchase pu
                            WHERE pu.product_id = p.product_id
                            ORDER BY pu.purchase_date DESC, pu.purchase_id DESC LIMIT 1),
                           p.cost_price) AS last_cost,
                       (SELECT pu.supplier FROM Purchase pu
                        WHERE pu.product_id = p.product_id AND pu.supplier IS NOT NULL
                        ORDER BY pu.purchase_date DESC, pu.purchase_id DESC LIMIT 1) AS last_supplier
                FROM Product p
                JOIN Category c ON c.category_id = p.category_id
                LEFT JOIN Inventory i ON i.product_id = p.product_id AND i.store_id = ?
                WHERE 1 = 1
                """);
        List<Object> args = new ArrayList<>();
        args.add(storeId);
        args.add(storeId);

        String text = Text.trimToEmpty(search);
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
        if (lowStockOnly) {
            sql.append(" AND i.quantity IS NOT NULL AND i.quantity <= ?");
            args.add(lowStockThreshold);
        }
        sql.append(" ORDER BY (i.quantity IS NULL), i.quantity <= ? DESC, p.product_name");
        args.add(lowStockThreshold);

        return jdbcTemplate.query(
                sql.toString(),
                (resultSet, rowNumber) -> {
                    Integer quantity = resultSet.getObject("quantity", Integer.class);
                    int productId = resultSet.getInt("product_id");
                    return new InventoryItemResponse(
                            productId,
                            resultSet.getString("product_name"),
                            resultSet.getString("brand"),
                            resultSet.getBigDecimal("price"),
                            resultSet.getInt("category_id"),
                            resultSet.getString("category_name"),
                            quantity,
                            quantity != null,
                            quantity != null && quantity <= lowStockThreshold,
                            images.urlFor(productId),
                            resultSet.getBigDecimal("last_cost"),
                            resultSet.getString("last_supplier"));
                },
                args.toArray());
    }

    @Transactional
    public ProductResponse restock(RestockRequest request) {
        requireStore(request.storeId());
        requireProduct(request.productId());

        jdbcTemplate.update("""
                INSERT INTO Inventory (store_id, product_id, quantity)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity)
                """,
                request.storeId(),
                request.productId(),
                request.quantity());
        recordPurchase(request.storeId(), request.productId(), request.quantity(), request.unitCost(),
                request.supplier());

        return findResponse(request.storeId(), request.productId());
    }

    @Transactional(readOnly = true)
    public List<PurchaseResponse> purchases(Integer storeId, String search, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT pu.purchase_id, pu.purchase_date, pu.store_id, s.city, pu.product_id,
                       p.product_name, pu.quantity, pu.unit_cost, pu.supplier
                FROM Purchase pu
                JOIN Store s ON s.store_id = pu.store_id
                JOIN Product p ON p.product_id = pu.product_id
                WHERE 1 = 1
                """);
        List<Object> args = new ArrayList<>();
        if (storeId != null) {
            sql.append(" AND pu.store_id = ?");
            args.add(storeId);
        }
        String text = Text.trimToEmpty(search);
        if (!text.isEmpty()) {
            sql.append(" AND (p.product_name LIKE ? OR COALESCE(pu.supplier, '') LIKE ?)");
            String pattern = Text.likePattern(text);
            args.add(pattern);
            args.add(pattern);
        }
        sql.append(" ORDER BY pu.purchase_date DESC, pu.purchase_id DESC LIMIT ?");
        args.add(limit);
        return jdbcTemplate.query(sql.toString(), (resultSet, rowNumber) -> {
            BigDecimal unitCost = resultSet.getBigDecimal("unit_cost");
            int quantity = resultSet.getInt("quantity");
            return new PurchaseResponse(
                    resultSet.getInt("purchase_id"),
                    resultSet.getDate("purchase_date").toLocalDate(),
                    resultSet.getInt("store_id"),
                    resultSet.getString("city"),
                    resultSet.getInt("product_id"),
                    resultSet.getString("product_name"),
                    quantity,
                    unitCost,
                    unitCost.multiply(BigDecimal.valueOf(quantity)),
                    resultSet.getString("supplier"));
        }, args.toArray());
    }

    private void recordPurchase(
            Integer storeId,
            Integer productId,
            Integer quantity,
            BigDecimal unitCost,
            String supplier) {
        int purchaseId = Ids.next(jdbcTemplate, "Purchase", "purchase_id");
        jdbcTemplate.update("""
                INSERT INTO Purchase
                    (purchase_id, purchase_date, store_id, product_id, quantity, unit_cost, supplier)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                purchaseId,
                Date.valueOf(LocalDate.now()),
                storeId,
                productId,
                quantity,
                unitCost,
                Text.trimToNull(supplier));
        jdbcTemplate.update(
                "UPDATE Product SET cost_price = ? WHERE product_id = ?",
                unitCost,
                productId);
    }

    private void requireUniqueProduct(String productName, String brand, Integer excludeProductId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM Product
                WHERE LOWER(product_name) = LOWER(?)
                  AND LOWER(COALESCE(brand, '')) = LOWER(?)
                  AND product_id <> ?
                """,
                Integer.class,
                productName.trim(),
                Text.trimToEmpty(brand),
                excludeProductId == null ? 0 : excludeProductId);
        if (count != null && count > 0) {
            throw new BadRequestException(
                    "A product named " + productName.trim() + " from this brand already exists");
        }
    }

    private ProductResponse findResponse(Integer storeId, Integer productId) {
        List<ProductResponse> products = jdbcTemplate.query("""
                SELECT p.product_id, p.product_name, p.brand, p.price,
                       p.category_id, c.category_name, i.quantity
                FROM Product p
                JOIN Category c ON c.category_id = p.category_id
                JOIN Inventory i ON i.product_id = p.product_id
                WHERE i.store_id = ? AND p.product_id = ?
                """,
                (resultSet, rowNumber) -> new ProductResponse(
                        resultSet.getInt("product_id"),
                        resultSet.getString("product_name"),
                        resultSet.getString("brand"),
                        resultSet.getBigDecimal("price"),
                        resultSet.getInt("category_id"),
                        resultSet.getString("category_name"),
                        resultSet.getInt("quantity"),
                        images.urlFor(productId)),
                storeId,
                productId);

        if (products.isEmpty()) {
            throw new ResourceNotFoundException("Product " + productId + " was not found");
        }
        return products.getFirst();
    }

    private void requireProduct(Integer productId) {
        requireRecord("Product", "product_id", productId, "Product");
    }

    private void requireCategory(Integer categoryId) {
        requireRecord("Category", "category_id", categoryId, "Category");
    }

    private void requireStore(Integer storeId) {
        requireRecord("Store", "store_id", storeId, "Store");
    }

    private void requireRecord(String table, String column, Integer id, String label) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?",
                Integer.class,
                id);
        if (count == null || count == 0) {
            throw new ResourceNotFoundException(label + " " + id + " was not found");
        }
    }
}
