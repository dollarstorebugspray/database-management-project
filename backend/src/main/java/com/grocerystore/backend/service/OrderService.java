package com.grocerystore.backend.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grocerystore.backend.dto.CreateOrderRequest;
import com.grocerystore.backend.dto.ItemChangeResponse;
import com.grocerystore.backend.dto.OrderItemResponse;
import com.grocerystore.backend.dto.OrderLineRequest;
import com.grocerystore.backend.dto.OrderResponse;
import com.grocerystore.backend.dto.OrderSearch;
import com.grocerystore.backend.exception.BadRequestException;
import com.grocerystore.backend.exception.InsufficientStockException;
import com.grocerystore.backend.exception.ResourceNotFoundException;
import com.grocerystore.backend.model.OrderStatus;
import com.grocerystore.backend.util.Ids;
import com.grocerystore.backend.util.Text;

@Service
public class OrderService {

    static final String BUSINESS_INVOICE = "Business Invoice";
    static final List<String> PAYMENT_METHODS =
            List.of("Credit Card", "Debit Card", "Cash at Pickup", BUSINESS_INVOICE);

    private static final int SALESPERSON_BALANCE_DAYS = 30;
    static final int RETURN_WINDOW_DAYS = 30;

    private static final String FIND_PRODUCT_FOR_UPDATE = """
            SELECT p.product_id, p.product_name, p.price, i.quantity
            FROM Product p
            JOIN Inventory i ON i.product_id = p.product_id
            WHERE i.store_id = ? AND p.product_id = ?
            FOR UPDATE
            """;

    private static final String SELECT_ORDER_HEADERS = """
            SELECT o.order_id, o.order_date, o.payment_method, o.order_status,
                   o.customer_id, c.name AS customer_name, c.customer_type,
                   o.store_id, s.city AS store_city,
                   o.salesperson_id, e.name AS salesperson_name
            FROM Orders o
            JOIN Customer c ON c.customer_id = o.customer_id
            JOIN Store s ON s.store_id = o.store_id
            LEFT JOIN Salesperson e ON e.salesperson_id = o.salesperson_id
            """;

    private final JdbcTemplate jdbcTemplate;

    public OrderService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request, Integer actingSalespersonId) {
        // Validate general order information
        validateRequest(request);
        String paymentMethod = normalizePaymentMethod(request.paymentMethod());
        // Verify the customer exists
        String customerType = requireCustomer(request.customerId());
        if (paymentMethod.equals(BUSINESS_INVOICE) && !customerType.equals("BUSINESS")) {
            throw new BadRequestException(BUSINESS_INVOICE + " is only available to business customers");
        }
        // Verify the store exists
        requireStore(request.storeId());
        Integer salespersonId = chooseSalesperson(request.storeId(), actingSalespersonId);

        // Lock inventory records in a consistent order
        List<OrderLineRequest> requestedItems = request.items().stream()
                .sorted(Comparator.comparing(OrderLineRequest::productId))
                .toList();

        List<PreparedLine> preparedLines = new ArrayList<>();
        for (OrderLineRequest item : requestedItems) {
            ProductStock product = findAndLockProduct(request.storeId(), item.productId());
            // Prevent purchases that exceed available inventory
            if (product.quantity() < item.quantity()) {
                throw new InsufficientStockException(
                        product.productName() + " has only " + product.quantity()
                                + " item(s) available, but " + item.quantity() + " were requested");
            }
            preparedLines.add(new PreparedLine(product, item.quantity()));
        }

        int orderId = Ids.next(jdbcTemplate, "Orders", "order_id");
        OrderStatus status = initialStatus(paymentMethod);
        Date today = Date.valueOf(LocalDate.now());

        // Create the general order record
        jdbcTemplate.update("""
                INSERT INTO Orders
                    (order_id, order_date, payment_method, order_status,
                     customer_id, salesperson_id, store_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                orderId,
                today,
                paymentMethod,
                status.name(),
                request.customerId(),
                salespersonId,
                request.storeId());

        for (PreparedLine line : preparedLines) {
            // Reduce available stock
            int updatedRows = jdbcTemplate.update("""
                    UPDATE Inventory
                    SET quantity = quantity - ?
                    WHERE store_id = ? AND product_id = ? AND quantity >= ?
                    """,
                    line.quantity(),
                    request.storeId(),
                    line.product().productId(),
                    line.quantity());

            if (updatedRows != 1) {
                throw new InsufficientStockException(
                        "Inventory changed while the order was being created. Please try again.");
            }

            // Create an OrderItem for each purchased product
            int productId = line.product().productId();
            jdbcTemplate.update("""
                    INSERT INTO OrderItem (order_id, product_id, quantity, sale_price, unit_cost)
                    VALUES (?, ?, ?, ?, %s)
                    """.formatted(UNIT_COST),
                    orderId,
                    productId,
                    line.quantity(),
                    line.product().price(),
                    productId, request.storeId(), today,
                    productId, today,
                    productId);
        }

        return findById(orderId);
    }

    static final String UNIT_COST = """
            COALESCE(
                (SELECT pu.unit_cost FROM Purchase pu
                 WHERE pu.product_id = ? AND pu.store_id = ? AND pu.purchase_date <= ?
                 ORDER BY pu.purchase_date DESC, pu.purchase_id DESC LIMIT 1),
                (SELECT pu.unit_cost FROM Purchase pu
                 WHERE pu.product_id = ? AND pu.purchase_date <= ?
                 ORDER BY pu.purchase_date DESC, pu.purchase_id DESC LIMIT 1),
                (SELECT p.cost_price FROM Product p WHERE p.product_id = ?))""";

    @Transactional(readOnly = true)
    public OrderResponse findById(Integer orderId) {
        List<OrderResponse> orders = loadOrders("o.order_id = ?", List.of(orderId), null);
        if (orders.isEmpty()) {
            throw new ResourceNotFoundException("Order " + orderId + " was not found");
        }
        return orders.getFirst();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findByCustomerId(Integer customerId) {
        requireCustomer(customerId);
        return loadOrders("o.customer_id = ?", List.of(customerId), null);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> search(OrderSearch criteria) {
        StringBuilder where = new StringBuilder("1 = 1");
        List<Object> args = new ArrayList<>();
        // Retrieve orders belonging to a store
        if (criteria.storeId() != null) {
            where.append(" AND o.store_id = ?");
            args.add(criteria.storeId());
        }
        String status = Text.trimToNull(criteria.status());
        if (status != null && !status.equalsIgnoreCase("ALL")) {
            where.append(" AND o.order_status = ?");
            args.add(OrderStatus.parse(status).name());
        }
        String text = Text.trimToEmpty(criteria.search());
        if (!text.isEmpty()) {
            if (text.matches("#?\\d{1,9}")) {
                where.append(" AND (o.order_id = ? OR c.name LIKE ?)");
                args.add(Integer.parseInt(text.replace("#", "")));
            } else {
                where.append(" AND (c.name LIKE ?)");
            }
            args.add(Text.likePattern(text));
        }
        int limit = criteria.limit() == null ? 100 : criteria.limit();
        return loadOrders(where.toString(), args, limit);
    }

    @Transactional
    public OrderResponse updateStatus(Integer orderId, String requestedStatus) {
        OrderStatus next = OrderStatus.parse(requestedStatus);
        OrderState order = lockOrder(orderId);
        order.status().requireTransitionTo(next);
        if (order.status() == next) {
            return findById(orderId);
        }

        if (next == OrderStatus.CANCELLED) {
            restoreInventory(orderId, order.storeId());
        }
        jdbcTemplate.update(
                "UPDATE Orders SET order_status = ? WHERE order_id = ?",
                next.name(),
                orderId);
        return findById(orderId);
    }

    @Transactional
    public OrderResponse cancelByCustomer(Integer orderId) {
        OrderState order = lockOrder(orderId);
        if (order.status() == OrderStatus.CANCELLED) {
            return findById(orderId);
        }
        if (!order.status().customerCanCancel()) {
            throw new BadRequestException(
                    "This order is already " + order.status().name().toLowerCase(Locale.ROOT)
                            + " and can no longer be cancelled online. Please contact the store.");
        }
        restoreInventory(orderId, order.storeId());
        jdbcTemplate.update(
                "UPDATE Orders SET order_status = 'CANCELLED' WHERE order_id = ?",
                orderId);
        return findById(orderId);
    }

    @Transactional
    public ItemChangeResponse cancelItems(Integer orderId, List<OrderLineRequest> lines, boolean byStaff) {
        Map<Integer, Integer> requested = requestedQuantities(lines);
        OrderState order = lockOrder(orderId);
        switch (order.status()) {
            case CANCELLED -> throw new BadRequestException("This order is already cancelled");
            case COMPLETED -> throw new BadRequestException(
                    "This order is completed. Items from a completed order can be returned at the store.");
            case PROCESSING -> {
                if (!byStaff) {
                    throw new BadRequestException(
                            "This order is already processing and can no longer be changed online. "
                                    + "Please contact the store.");
                }
            }
            default -> {
            }
        }
        return removeItems(orderId, order, requested, true);
    }

    @Transactional
    public ItemChangeResponse returnItems(Integer orderId, List<OrderLineRequest> lines, boolean restock) {
        Map<Integer, Integer> requested = requestedQuantities(lines);
        OrderState order = lockOrder(orderId);
        if (order.status() != OrderStatus.COMPLETED) {
            throw new BadRequestException(
                    "Only completed orders can be returned. Cancel items on an open order instead.");
        }
        requireReturnWindow(order.orderDate(), LocalDate.now());
        return removeItems(orderId, order, requested, restock);
    }

    @Transactional
    public OrderResponse assignSalesperson(Integer orderId, Integer salespersonId) {
        OrderState order = lockOrder(orderId);
        if (order.status() == OrderStatus.CANCELLED) {
            throw new BadRequestException("A cancelled order cannot be reassigned");
        }
        List<Integer> stores = jdbcTemplate.queryForList(
                "SELECT store_id FROM Salesperson WHERE salesperson_id = ?",
                Integer.class,
                salespersonId);
        if (stores.isEmpty()) {
            throw new ResourceNotFoundException("Salesperson " + salespersonId + " was not found");
        }
        if (!stores.getFirst().equals(order.storeId())) {
            throw new BadRequestException(
                    "Salesperson " + salespersonId + " does not work at store " + order.storeId());
        }
        jdbcTemplate.update(
                "UPDATE Orders SET salesperson_id = ? WHERE order_id = ?",
                salespersonId,
                orderId);
        return findById(orderId);
    }

    static Map<Integer, Integer> requestedQuantities(List<OrderLineRequest> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new BadRequestException("Choose at least one product");
        }
        Map<Integer, Integer> requested = new TreeMap<>();
        for (OrderLineRequest line : lines) {
            if (line == null || line.productId() == null || line.quantity() == null
                    || line.productId() < 1 || line.quantity() < 1) {
                throw new BadRequestException("Each item needs a valid productId and a quantity of at least 1");
            }
            if (requested.put(line.productId(), line.quantity()) != null) {
                throw new BadRequestException("Product " + line.productId() + " appears more than once");
            }
        }
        return requested;
    }

    static Map<Integer, Integer> remainingAfter(
            Map<Integer, Integer> onOrder,
            Map<Integer, Integer> requested,
            Map<Integer, String> names) {
        Map<Integer, Integer> remaining = new TreeMap<>(onOrder);
        requested.forEach((productId, quantity) -> {
            Integer current = onOrder.get(productId);
            if (current == null) {
                throw new BadRequestException("Product " + productId + " is not on this order");
            }
            if (quantity > current) {
                throw new BadRequestException(
                        "Only " + current + " of " + names.getOrDefault(productId, "product " + productId)
                                + " are on this order");
            }
            remaining.put(productId, current - quantity);
        });
        return remaining;
    }

    static void requireReturnWindow(LocalDate orderDate, LocalDate today) {
        if (orderDate.isBefore(today.minusDays(RETURN_WINDOW_DAYS))) {
            throw new BadRequestException(
                    "Returns are accepted within " + RETURN_WINDOW_DAYS + " days of the order date");
        }
    }

    static String normalizePaymentMethod(String paymentMethod) {
        String value = Text.trimToEmpty(paymentMethod);
        return PAYMENT_METHODS.stream()
                .filter(method -> method.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "paymentMethod must be one of " + String.join(", ", PAYMENT_METHODS)));
    }

    static OrderStatus initialStatus(String paymentMethod) {
        return paymentMethod.endsWith("Card") ? OrderStatus.PAID : OrderStatus.PENDING;
    }

    private void validateRequest(CreateOrderRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new BadRequestException("An order must contain at least one product");
        }

        // Make sure the order does not repeat any product
        Set<Integer> productIds = new HashSet<>();
        for (OrderLineRequest item : request.items()) {
            if (item == null || item.productId() == null || item.quantity() == null
                    || item.productId() < 1 || item.quantity() < 1) {
                throw new BadRequestException("Each order item needs a valid productId and quantity");
            }
            if (!productIds.add(item.productId())) {
                throw new BadRequestException(
                        "Product " + item.productId() + " appears more than once in the order");
            }
        }
    }

    Integer chooseSalesperson(Integer storeId, Integer actingSalespersonId) {
        // Verify the salesperson, if provided
        if (actingSalespersonId != null) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM Salesperson WHERE salesperson_id = ? AND store_id = ?",
                    Integer.class,
                    actingSalespersonId,
                    storeId);
            if (count != null && count > 0) {
                return actingSalespersonId;
            }
        }

        List<Integer> candidates = jdbcTemplate.queryForList("""
                SELECT e.salesperson_id
                FROM Salesperson e
                LEFT JOIN Orders o
                       ON o.salesperson_id = e.salesperson_id AND o.order_date >= ?
                WHERE e.store_id = ?
                GROUP BY e.salesperson_id, e.job_title
                ORDER BY CASE WHEN LOWER(e.job_title) LIKE '%manager%' THEN 1 ELSE 0 END,
                         COUNT(o.order_id),
                         e.salesperson_id
                LIMIT 1
                """,
                Integer.class,
                Date.valueOf(LocalDate.now().minusDays(SALESPERSON_BALANCE_DAYS)),
                storeId);
        if (candidates.isEmpty()) {
            throw new BadRequestException(
                    "Store " + storeId + " has no staff yet, so it can't take orders. Please choose another store.");
        }
        return candidates.getFirst();
    }

    private ItemChangeResponse removeItems(
            Integer orderId,
            OrderState order,
            Map<Integer, Integer> requested,
            boolean restock) {
        Map<Integer, Integer> onOrder = new TreeMap<>();
        Map<Integer, BigDecimal> prices = new HashMap<>();
        Map<Integer, String> names = new HashMap<>();
        jdbcTemplate.query("""
                SELECT oi.product_id, p.product_name, oi.quantity, oi.sale_price
                FROM OrderItem oi
                JOIN Product p ON p.product_id = oi.product_id
                WHERE oi.order_id = ?
                FOR UPDATE OF oi
                """,
                resultSet -> {
                    int productId = resultSet.getInt("product_id");
                    onOrder.put(productId, resultSet.getInt("quantity"));
                    prices.put(productId, resultSet.getBigDecimal("sale_price"));
                    names.put(productId, resultSet.getString("product_name"));
                },
                orderId);

        Map<Integer, Integer> remaining = remainingAfter(onOrder, requested, names);
        BigDecimal refund = BigDecimal.ZERO;
        int units = 0;
        for (Map.Entry<Integer, Integer> line : requested.entrySet()) {
            refund = refund.add(prices.get(line.getKey()).multiply(BigDecimal.valueOf(line.getValue())));
            units += line.getValue();
        }

        boolean nothingLeft = remaining.values().stream().allMatch(quantity -> quantity == 0);
        if (nothingLeft) {
            if (restock) {
                restoreInventory(orderId, order.storeId());
            }
            jdbcTemplate.update("UPDATE Orders SET order_status = 'CANCELLED' WHERE order_id = ?", orderId);
        } else {
            for (Map.Entry<Integer, Integer> line : requested.entrySet()) {
                int productId = line.getKey();
                int left = remaining.get(productId);
                if (left == 0) {
                    jdbcTemplate.update(
                            "DELETE FROM OrderItem WHERE order_id = ? AND product_id = ?",
                            orderId,
                            productId);
                } else {
                    jdbcTemplate.update(
                            "UPDATE OrderItem SET quantity = ? WHERE order_id = ? AND product_id = ?",
                            left,
                            orderId,
                            productId);
                }
                if (restock) {
                    jdbcTemplate.update("""
                            INSERT INTO Inventory (store_id, product_id, quantity)
                            VALUES (?, ?, ?)
                            ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity)
                            """,
                            order.storeId(),
                            productId,
                            line.getValue());
                }
            }
        }
        return new ItemChangeResponse(findById(orderId), refund, units, nothingLeft);
    }

    private void restoreInventory(Integer orderId, Integer storeId) {
        jdbcTemplate.update("""
                UPDATE Inventory inventory
                JOIN OrderItem item ON item.product_id = inventory.product_id
                SET inventory.quantity = inventory.quantity + item.quantity
                WHERE inventory.store_id = ? AND item.order_id = ?
                """,
                storeId,
                orderId);
    }

    private OrderState lockOrder(Integer orderId) {
        List<OrderState> orders = jdbcTemplate.query("""
                SELECT store_id, order_status, order_date
                FROM Orders
                WHERE order_id = ?
                FOR UPDATE
                """,
                (resultSet, rowNumber) -> new OrderState(
                        resultSet.getInt("store_id"),
                        OrderStatus.parse(resultSet.getString("order_status")),
                        resultSet.getDate("order_date").toLocalDate()),
                orderId);

        if (orders.isEmpty()) {
            throw new ResourceNotFoundException("Order " + orderId + " was not found");
        }
        return orders.getFirst();
    }

    private List<OrderResponse> loadOrders(String where, List<Object> args, Integer limit) {
        List<Object> headerArgs = new ArrayList<>(args);
        String sql = SELECT_ORDER_HEADERS + " WHERE " + where
                + " ORDER BY o.order_date DESC, o.order_id DESC";
        if (limit != null) {
            sql += " LIMIT ?";
            headerArgs.add(limit);
        }

        List<OrderHeader> headers = jdbcTemplate.query(
                sql,
                (resultSet, rowNumber) -> new OrderHeader(
                        resultSet.getInt("order_id"),
                        resultSet.getDate("order_date").toLocalDate(),
                        resultSet.getString("payment_method"),
                        resultSet.getString("order_status"),
                        resultSet.getInt("customer_id"),
                        resultSet.getString("customer_name"),
                        resultSet.getString("customer_type"),
                        resultSet.getInt("store_id"),
                        resultSet.getString("store_city"),
                        resultSet.getObject("salesperson_id", Integer.class),
                        resultSet.getString("salesperson_name")),
                headerArgs.toArray());
        if (headers.isEmpty()) {
            return List.of();
        }

        List<Integer> orderIds = headers.stream().map(OrderHeader::orderId).toList();
        String placeholders = orderIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        // Retrieve all products belonging to a specific order
        Map<Integer, List<OrderItemResponse>> itemsByOrder = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT oi.order_id, oi.product_id, p.product_name, oi.quantity, oi.sale_price
                FROM OrderItem oi
                JOIN Product p ON p.product_id = oi.product_id
                WHERE oi.order_id IN (%s)
                ORDER BY oi.order_id, p.product_name
                """.formatted(placeholders),
                resultSet -> {
                    BigDecimal price = resultSet.getBigDecimal("sale_price");
                    int quantity = resultSet.getInt("quantity");
                    itemsByOrder
                            .computeIfAbsent(resultSet.getInt("order_id"), key -> new ArrayList<>())
                            .add(new OrderItemResponse(
                                    resultSet.getInt("product_id"),
                                    resultSet.getString("product_name"),
                                    quantity,
                                    price,
                                    price.multiply(BigDecimal.valueOf(quantity))));
                },
                orderIds.toArray());

        return headers.stream()
                .map(header -> {
                    List<OrderItemResponse> items = itemsByOrder.getOrDefault(header.orderId(), List.of());
                    BigDecimal total = items.stream()
                            .map(OrderItemResponse::lineTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    int itemCount = items.stream().mapToInt(OrderItemResponse::quantity).sum();
                    return new OrderResponse(
                            header.orderId(),
                            header.orderDate(),
                            header.customerId(),
                            header.customerName(),
                            header.customerType(),
                            header.storeId(),
                            header.storeCity(),
                            header.salespersonId(),
                            header.salespersonName(),
                            header.paymentMethod(),
                            header.orderStatus(),
                            List.copyOf(items),
                            itemCount,
                            total);
                })
                .toList();
    }

    private String requireCustomer(Integer customerId) {
        List<String> types = jdbcTemplate.queryForList(
                "SELECT customer_type FROM Customer WHERE customer_id = ?",
                String.class,
                customerId);
        if (types.isEmpty()) {
            throw new ResourceNotFoundException("Customer " + customerId + " was not found");
        }
        return types.getFirst();
    }

    // Check whether a referenced database record exists
    private void requireStore(Integer storeId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM Store WHERE store_id = ?",
                Integer.class,
                storeId);
        if (count == null || count == 0) {
            throw new ResourceNotFoundException("Store " + storeId + " was not found");
        }
    }

    private ProductStock findAndLockProduct(Integer storeId, Integer productId) {
        List<ProductStock> products = jdbcTemplate.query(
                FIND_PRODUCT_FOR_UPDATE,
                (resultSet, rowNumber) -> new ProductStock(
                        resultSet.getInt("product_id"),
                        resultSet.getString("product_name"),
                        resultSet.getBigDecimal("price"),
                        resultSet.getInt("quantity")),
                storeId,
                productId);

        // Check whether the store carries the product
        if (products.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Product " + productId + " is not available at store " + storeId);
        }
        return products.getFirst();
    }

    private record ProductStock(
            Integer productId,
            String productName,
            BigDecimal price,
            Integer quantity) {
    }

    private record PreparedLine(ProductStock product, Integer quantity) {
    }

    private record OrderHeader(
            Integer orderId,
            LocalDate orderDate,
            String paymentMethod,
            String orderStatus,
            Integer customerId,
            String customerName,
            String customerType,
            Integer storeId,
            String storeCity,
            Integer salespersonId,
            String salespersonName) {
    }

    private record OrderState(Integer storeId, OrderStatus status, LocalDate orderDate) {
    }
}
