package com.grocerystore.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grocerystore.backend.dto.ReportFilter;
import com.grocerystore.backend.dto.Reports.BusinessBuyer;
import com.grocerystore.backend.dto.Reports.BusinessBuyers;
import com.grocerystore.backend.dto.Reports.BusinessCategoryPurchase;
import com.grocerystore.backend.dto.Reports.CategorySales;
import com.grocerystore.backend.dto.Reports.CustomerSegments;
import com.grocerystore.backend.dto.Reports.MonthlySales;
import com.grocerystore.backend.dto.Reports.Overview;
import com.grocerystore.backend.dto.Reports.PaymentMethodSales;
import com.grocerystore.backend.dto.Reports.ProductSales;
import com.grocerystore.backend.dto.Reports.RegionSales;
import com.grocerystore.backend.dto.Reports.SalespersonSales;
import com.grocerystore.backend.dto.Reports.SegmentSales;
import com.grocerystore.backend.dto.Reports.StoreSales;
import com.grocerystore.backend.dto.Reports.TopCustomer;
import com.grocerystore.backend.exception.ResourceNotFoundException;
import com.grocerystore.backend.model.OrderStatus;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private static final String REVENUE = "oi.quantity * oi.sale_price";

    static final String PAID_STATUSES = Arrays.stream(OrderStatus.values())
            .filter(OrderStatus::isPaid)
            .map(status -> "'" + status.name() + "'")
            .collect(Collectors.joining(", ", "(", ")"));

    private static final String SALES_FROM = """
             FROM Orders o
             JOIN OrderItem oi ON oi.order_id = o.order_id
             JOIN Product p ON p.product_id = oi.product_id
             JOIN Store s ON s.store_id = o.store_id
            """;

    static final List<String> AGE_BANDS =
            List.of("Under 25", "25-34", "35-49", "50-64", "65+", "Unknown");
    static final List<String> INCOME_BANDS =
            List.of("Under $40k", "$40k-$75k", "$75k-$100k", "$100k+", "Unknown");

    private static final String PROFIT = "oi.quantity * (oi.sale_price - oi.unit_cost)";

    private final JdbcTemplate jdbcTemplate;

    public ReportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Overview overview(ReportFilter filter) {
        Query query = salesQuery("""
                SELECT COUNT(DISTINCT o.order_id) AS order_count,
                       COALESCE(SUM(oi.quantity), 0) AS units,
                       COALESCE(SUM(%s), 0) AS revenue,
                       COALESCE(SUM(CASE WHEN o.order_status IN %s THEN %s END), 0) AS collected,
                       COALESCE(SUM(%s), 0) AS profit,
                       COUNT(DISTINCT o.customer_id) AS customer_count
                """.formatted(REVENUE, PAID_STATUSES, REVENUE, PROFIT), "", filter);

        StringBuilder cancelledSql = new StringBuilder("""
                SELECT COUNT(*)
                FROM Orders o
                JOIN Store s ON s.store_id = o.store_id
                WHERE o.order_status = 'CANCELLED'
                """);
        List<Object> cancelledArgs = new ArrayList<>();
        filter.appendTo(cancelledSql, cancelledArgs);
        Long cancelled = jdbcTemplate.queryForObject(
                cancelledSql.toString(), Long.class, cancelledArgs.toArray());

        return jdbcTemplate.queryForObject(query.sql(), (resultSet, rowNumber) -> {
            long orders = resultSet.getLong("order_count");
            BigDecimal revenue = money(resultSet.getBigDecimal("revenue"));
            BigDecimal profit = money(resultSet.getBigDecimal("profit"));
            return new Overview(
                    revenue,
                    money(resultSet.getBigDecimal("collected")),
                    profit,
                    orders,
                    resultSet.getLong("units"),
                    average(revenue, orders),
                    resultSet.getLong("customer_count"),
                    cancelled == null ? 0 : cancelled);
        }, query.args());
    }

    // 1. Aggregate product sales and gross profit
    public List<ProductSales> products(ReportFilter filter, Integer categoryId, Integer limit) {
        Query sales = salesQuery("""
                SELECT oi.product_id,
                       SUM(oi.quantity) AS units,
                       COUNT(DISTINCT o.order_id) AS orders,
                       SUM(%s) AS revenue,
                       SUM(%s) AS profit
                """.formatted(REVENUE, PROFIT), "", filter);

        StringBuilder sql = new StringBuilder("""
                SELECT p.product_id, p.product_name, p.brand, c.category_name,
                       COALESCE(x.units, 0) AS units,
                       COALESCE(x.orders, 0) AS orders,
                       COALESCE(x.revenue, 0) AS revenue,
                       COALESCE(x.profit, 0) AS profit
                FROM Product p
                JOIN Category c ON c.category_id = p.category_id
                LEFT JOIN (
                """)
                .append(sales.sql()).append(" GROUP BY oi.product_id")
                .append(") x ON x.product_id = p.product_id WHERE 1 = 1");
        List<Object> args = new ArrayList<>(List.of(sales.args()));
        if (categoryId != null) {
            sql.append(" AND p.category_id = ?");
            args.add(categoryId);
        }
        sql.append(" ORDER BY revenue DESC, p.product_name");
        if (limit != null) {
            sql.append(" LIMIT ?");
            args.add(limit);
        }

        return jdbcTemplate.query(sql.toString(), (resultSet, rowNumber) -> {
            BigDecimal revenue = money(resultSet.getBigDecimal("revenue"));
            BigDecimal profit = money(resultSet.getBigDecimal("profit"));
            return new ProductSales(
                    resultSet.getInt("product_id"),
                    resultSet.getString("product_name"),
                    resultSet.getString("brand"),
                    resultSet.getString("category_name"),
                    resultSet.getLong("units"),
                    resultSet.getLong("orders"),
                    revenue,
                    profit);
        }, args.toArray());
    }

    // 2. Rank product categories by sales
    public List<CategorySales> categories(ReportFilter filter) {
        Query sales = salesQuery("""
                SELECT p.category_id,
                       SUM(oi.quantity) AS units,
                       COUNT(DISTINCT o.order_id) AS orders,
                       SUM(%s) AS revenue,
                       SUM(%s) AS profit
                """.formatted(REVENUE, PROFIT), "", filter);

        String sql = """
                SELECT c.category_id, c.category_name,
                       (SELECT COUNT(*) FROM Product pc WHERE pc.category_id = c.category_id) AS product_count,
                       COALESCE(x.units, 0) AS units,
                       COALESCE(x.orders, 0) AS orders,
                       COALESCE(x.revenue, 0) AS revenue,
                       COALESCE(x.profit, 0) AS profit
                FROM Category c
                LEFT JOIN (
                """ + sales.sql() + " GROUP BY p.category_id"
                + ") x ON x.category_id = c.category_id"
                + " ORDER BY revenue DESC, c.category_name";

        List<CategorySales> rows = jdbcTemplate.query(sql, (resultSet, rowNumber) -> new CategorySales(
                resultSet.getInt("category_id"),
                resultSet.getString("category_name"),
                resultSet.getLong("product_count"),
                resultSet.getLong("units"),
                resultSet.getLong("orders"),
                money(resultSet.getBigDecimal("revenue")),
                money(resultSet.getBigDecimal("profit")),
                BigDecimal.ZERO), sales.args());

        BigDecimal total = sum(rows, CategorySales::revenue);
        return rows.stream()
                .map(row -> new CategorySales(
                        row.categoryId(), row.categoryName(), row.productCount(), row.unitsSold(),
                        row.orderCount(), row.revenue(), row.profit(),
                        percent(row.revenue(), total)))
                .toList();
    }

    // 3. Compare regions by sales volume and revenue
    public List<RegionSales> regions(ReportFilter filter) {
        Query sales = salesQuery("""
                SELECT s.region_id,
                       COUNT(DISTINCT o.order_id) AS orders,
                       SUM(oi.quantity) AS units,
                       SUM(%s) AS revenue,
                       SUM(%s) AS profit
                """.formatted(REVENUE, PROFIT), "", filter);

        StringBuilder sql = new StringBuilder("""
                SELECT r.region_id, r.region_name, r.region_manager,
                       (SELECT COUNT(*) FROM Store rs WHERE rs.region_id = r.region_id) AS store_count,
                       COALESCE(x.orders, 0) AS orders,
                       COALESCE(x.units, 0) AS units,
                       COALESCE(x.revenue, 0) AS revenue,
                       COALESCE(x.profit, 0) AS profit
                FROM Region r
                LEFT JOIN (
                """)
                .append(sales.sql()).append(" GROUP BY s.region_id")
                .append(") x ON x.region_id = r.region_id WHERE 1 = 1");
        List<Object> args = new ArrayList<>(List.of(sales.args()));
        if (filter.regionId() != null) {
            sql.append(" AND r.region_id = ?");
            args.add(filter.regionId());
        }
        sql.append(" ORDER BY revenue DESC, r.region_name");

        List<RegionSales> rows = jdbcTemplate.query(sql.toString(), (resultSet, rowNumber) -> {
            long orders = resultSet.getLong("orders");
            BigDecimal revenue = money(resultSet.getBigDecimal("revenue"));
            return new RegionSales(
                    resultSet.getInt("region_id"),
                    resultSet.getString("region_name"),
                    resultSet.getString("region_manager"),
                    resultSet.getLong("store_count"),
                    orders,
                    resultSet.getLong("units"),
                    revenue,
                    money(resultSet.getBigDecimal("profit")),
                    average(revenue, orders),
                    BigDecimal.ZERO);
        }, args.toArray());

        BigDecimal total = sum(rows, RegionSales::revenue);
        return rows.stream()
                .map(row -> new RegionSales(
                        row.regionId(), row.regionName(), row.regionManager(), row.storeCount(),
                        row.orderCount(), row.unitsSold(), row.revenue(), row.profit(),
                        row.averageOrderValue(), percent(row.revenue(), total)))
                .toList();
    }

    public List<StoreSales> stores(ReportFilter filter) {
        Query sales = salesQuery("""
                SELECT o.store_id,
                       COUNT(DISTINCT o.order_id) AS orders,
                       SUM(oi.quantity) AS units,
                       SUM(%s) AS revenue,
                       SUM(%s) AS profit
                """.formatted(REVENUE, PROFIT), "", filter);

        StringBuilder sql = new StringBuilder("""
                SELECT st.store_id, st.street, st.city, st.manager, r.region_name,
                       (SELECT COUNT(*) FROM Salesperson e WHERE e.store_id = st.store_id) AS salesperson_count,
                       COALESCE(x.orders, 0) AS orders,
                       COALESCE(x.units, 0) AS units,
                       COALESCE(x.revenue, 0) AS revenue,
                       COALESCE(x.profit, 0) AS profit
                FROM Store st
                JOIN Region r ON r.region_id = st.region_id
                LEFT JOIN (
                """)
                .append(sales.sql()).append(" GROUP BY o.store_id")
                .append(") x ON x.store_id = st.store_id WHERE 1 = 1");
        List<Object> args = new ArrayList<>(List.of(sales.args()));
        appendLocation(sql, args, filter, "st");
        sql.append(" ORDER BY revenue DESC, st.store_id");

        return jdbcTemplate.query(sql.toString(), (resultSet, rowNumber) -> new StoreSales(
                resultSet.getInt("store_id"),
                resultSet.getString("street"),
                resultSet.getString("city"),
                resultSet.getString("region_name"),
                resultSet.getString("manager"),
                resultSet.getLong("salesperson_count"),
                resultSet.getLong("orders"),
                resultSet.getLong("units"),
                money(resultSet.getBigDecimal("revenue")),
                money(resultSet.getBigDecimal("profit"))), args.toArray());
    }

    // 4. Businesses purchasing a specified product
    public BusinessBuyers businessBuyers(
            ReportFilter filter,
            Integer productId,
            Integer categoryId,
            int limit) {
        String subject = describeSubject(productId, categoryId);
        StringBuilder itemFilter = new StringBuilder();
        List<Object> itemArgs = new ArrayList<>();
        if (productId != null) {
            itemFilter.append(" AND oi.product_id = ?");
            itemArgs.add(productId);
        }
        if (categoryId != null) {
            itemFilter.append(" AND p.category_id = ?");
            itemArgs.add(categoryId);
        }
        String businessJoin = """
                 JOIN Customer c ON c.customer_id = o.customer_id
                 JOIN BusinessCustomer b ON b.customer_id = c.customer_id
                """;

        Query buyersQuery = salesQuery("""
                SELECT c.customer_id, c.name, b.business_category, c.city,
                       COUNT(DISTINCT o.order_id) AS orders,
                       SUM(oi.quantity) AS units,
                       SUM(%s) AS spent,
                       MAX(o.order_date) AS last_order
                """.formatted(REVENUE), businessJoin, filter);
        List<Object> buyerArgs = new ArrayList<>(List.of(buyersQuery.args()));
        buyerArgs.addAll(itemArgs);
        buyerArgs.add(limit);
        List<BusinessBuyer> buyers = jdbcTemplate.query(
                buyersQuery.sql() + itemFilter
                        + " GROUP BY c.customer_id, c.name, b.business_category, c.city"
                        + " ORDER BY units DESC, spent DESC LIMIT ?",
                (resultSet, rowNumber) -> {
                    Date lastOrder = resultSet.getDate("last_order");
                    return new BusinessBuyer(
                            resultSet.getInt("customer_id"),
                            resultSet.getString("name"),
                            resultSet.getString("business_category"),
                            resultSet.getString("city"),
                            resultSet.getLong("orders"),
                            resultSet.getLong("units"),
                            money(resultSet.getBigDecimal("spent")),
                            lastOrder == null ? null : lastOrder.toLocalDate());
                },
                buyerArgs.toArray());

        Query categoryQuery = salesQuery("""
                SELECT b.business_category,
                       COUNT(DISTINCT c.customer_id) AS customers,
                       SUM(oi.quantity) AS units,
                       SUM(%s) AS spent
                """.formatted(REVENUE), businessJoin, filter);
        List<Object> categoryArgs = new ArrayList<>(List.of(categoryQuery.args()));
        categoryArgs.addAll(itemArgs);
        List<BusinessCategoryPurchase> byCategory = jdbcTemplate.query(
                categoryQuery.sql() + itemFilter
                        + " GROUP BY b.business_category ORDER BY units DESC, spent DESC",
                (resultSet, rowNumber) -> new BusinessCategoryPurchase(
                        resultSet.getString("business_category"),
                        resultSet.getLong("customers"),
                        resultSet.getLong("units"),
                        money(resultSet.getBigDecimal("spent"))),
                categoryArgs.toArray());

        return new BusinessBuyers(subject, buyers, byCategory);
    }

    public List<SalespersonSales> salespeople(ReportFilter filter) {
        Query sales = salesQuery("""
                SELECT o.salesperson_id,
                       COUNT(DISTINCT o.order_id) AS orders,
                       SUM(oi.quantity) AS units,
                       SUM(%s) AS revenue
                """.formatted(REVENUE), "", filter);

        StringBuilder sql = new StringBuilder("""
                SELECT e.salesperson_id, e.name, e.job_title, e.store_id, st.city,
                       COALESCE(x.orders, 0) AS orders,
                       COALESCE(x.units, 0) AS units,
                       COALESCE(x.revenue, 0) AS revenue
                FROM Salesperson e
                JOIN Store st ON st.store_id = e.store_id
                LEFT JOIN (
                """)
                .append(sales.sql()).append(" AND o.salesperson_id IS NOT NULL GROUP BY o.salesperson_id")
                .append(") x ON x.salesperson_id = e.salesperson_id WHERE 1 = 1");
        List<Object> args = new ArrayList<>(List.of(sales.args()));
        appendLocation(sql, args, filter, "st");
        sql.append(" ORDER BY revenue DESC, e.name");

        return jdbcTemplate.query(sql.toString(), (resultSet, rowNumber) -> {
            long orders = resultSet.getLong("orders");
            BigDecimal revenue = money(resultSet.getBigDecimal("revenue"));
            return new SalespersonSales(
                    resultSet.getInt("salesperson_id"),
                    resultSet.getString("name"),
                    resultSet.getString("job_title"),
                    resultSet.getInt("store_id"),
                    resultSet.getString("city"),
                    orders,
                    resultSet.getLong("units"),
                    revenue,
                    average(revenue, orders));
        }, args.toArray());
    }

    public List<MonthlySales> monthly(ReportFilter filter) {
        Query query = salesQuery("""
                SELECT DATE_FORMAT(o.order_date, '%%Y-%%m') AS sales_month,
                       COUNT(DISTINCT o.order_id) AS orders,
                       SUM(oi.quantity) AS units,
                       SUM(%s) AS revenue,
                       SUM(%s) AS profit
                """.formatted(REVENUE, PROFIT), "", filter);
        return jdbcTemplate.query(
                query.sql() + " GROUP BY sales_month ORDER BY sales_month",
                (resultSet, rowNumber) -> new MonthlySales(
                        resultSet.getString("sales_month"),
                        resultSet.getLong("orders"),
                        resultSet.getLong("units"),
                        money(resultSet.getBigDecimal("revenue")),
                        money(resultSet.getBigDecimal("profit"))),
                query.args());
    }

    public CustomerSegments customerSegments(ReportFilter filter) {
        String select = """
                SELECT %s AS segment,
                       COUNT(DISTINCT o.customer_id) AS customers,
                       COUNT(DISTINCT o.order_id) AS orders,
                       SUM(%s) AS revenue
                """;

        List<SegmentSales> types = segmentRows(salesQuery(
                select.formatted("c.customer_type", REVENUE),
                " JOIN Customer c ON c.customer_id = o.customer_id ",
                filter), List.of("HOME", "BUSINESS"));

        String ageBand = """
                CASE WHEN h.age IS NULL THEN 'Unknown'
                     WHEN h.age < 25 THEN 'Under 25'
                     WHEN h.age < 35 THEN '25-34'
                     WHEN h.age < 50 THEN '35-49'
                     WHEN h.age < 65 THEN '50-64'
                     ELSE '65+' END""";
        String incomeBand = """
                CASE WHEN h.income IS NULL THEN 'Unknown'
                     WHEN h.income < 40000 THEN 'Under $40k'
                     WHEN h.income < 75000 THEN '$40k-$75k'
                     WHEN h.income < 100000 THEN '$75k-$100k'
                     ELSE '$100k+' END""";
        String homeJoin = " JOIN HomeCustomer h ON h.customer_id = o.customer_id ";

        List<SegmentSales> ages = segmentRows(
                salesQuery(select.formatted(ageBand, REVENUE), homeJoin, filter), AGE_BANDS);
        List<SegmentSales> incomes = segmentRows(
                salesQuery(select.formatted(incomeBand, REVENUE), homeJoin, filter), INCOME_BANDS);
        return new CustomerSegments(types, ages, incomes);
    }

    public List<TopCustomer> topCustomers(ReportFilter filter, int limit) {
        Query query = salesQuery("""
                SELECT c.customer_id, c.name, c.customer_type,
                       COUNT(DISTINCT o.order_id) AS orders,
                       SUM(oi.quantity) AS units,
                       SUM(%s) AS revenue,
                       MAX(o.order_date) AS last_order
                """.formatted(REVENUE), " JOIN Customer c ON c.customer_id = o.customer_id ", filter);
        List<Object> args = new ArrayList<>(List.of(query.args()));
        args.add(limit);
        return jdbcTemplate.query(
                query.sql() + " GROUP BY c.customer_id, c.name, c.customer_type"
                        + " ORDER BY revenue DESC LIMIT ?",
                (resultSet, rowNumber) -> {
                    Date lastOrder = resultSet.getDate("last_order");
                    return new TopCustomer(
                            resultSet.getInt("customer_id"),
                            resultSet.getString("name"),
                            resultSet.getString("customer_type"),
                            resultSet.getLong("orders"),
                            resultSet.getLong("units"),
                            money(resultSet.getBigDecimal("revenue")),
                            lastOrder == null ? null : lastOrder.toLocalDate());
                },
                args.toArray());
    }

    public List<PaymentMethodSales> paymentMethods(ReportFilter filter) {
        Query query = salesQuery("""
                SELECT o.payment_method,
                       COUNT(DISTINCT o.order_id) AS orders,
                       SUM(%s) AS revenue
                """.formatted(REVENUE), "", filter);
        List<PaymentMethodSales> rows = jdbcTemplate.query(
                query.sql() + " GROUP BY o.payment_method ORDER BY revenue DESC",
                (resultSet, rowNumber) -> new PaymentMethodSales(
                        resultSet.getString("payment_method"),
                        resultSet.getLong("orders"),
                        money(resultSet.getBigDecimal("revenue")),
                        BigDecimal.ZERO),
                query.args());
        BigDecimal total = sum(rows, PaymentMethodSales::revenue);
        return rows.stream()
                .map(row -> new PaymentMethodSales(
                        row.paymentMethod(), row.orderCount(), row.revenue(),
                        percent(row.revenue(), total)))
                .toList();
    }

    private List<SegmentSales> segmentRows(Query query, List<String> order) {
        List<SegmentSales> rows = jdbcTemplate.query(
                query.sql() + " GROUP BY segment",
                (resultSet, rowNumber) -> {
                    long orders = resultSet.getLong("orders");
                    BigDecimal revenue = money(resultSet.getBigDecimal("revenue"));
                    return new SegmentSales(
                            resultSet.getString("segment"),
                            resultSet.getLong("customers"),
                            orders,
                            revenue,
                            average(revenue, orders),
                            BigDecimal.ZERO);
                },
                query.args());
        BigDecimal total = sum(rows, SegmentSales::revenue);
        Map<String, Integer> position = new HashMap<>();
        for (int index = 0; index < order.size(); index++) {
            position.put(order.get(index), index);
        }
        return rows.stream()
                .map(row -> new SegmentSales(
                        row.segment(), row.customerCount(), row.orderCount(), row.revenue(),
                        row.averageOrderValue(), percent(row.revenue(), total)))
                .sorted(Comparator.comparing(row -> position.getOrDefault(row.segment(), order.size())))
                .toList();
    }

    private String describeSubject(Integer productId, Integer categoryId) {
        if (productId != null) {
            List<String> names = jdbcTemplate.queryForList(
                    "SELECT product_name FROM Product WHERE product_id = ?", String.class, productId);
            if (names.isEmpty()) {
                throw new ResourceNotFoundException("Product " + productId + " was not found");
            }
            return names.getFirst();
        }
        if (categoryId != null) {
            List<String> names = jdbcTemplate.queryForList(
                    "SELECT category_name FROM Category WHERE category_id = ?", String.class, categoryId);
            if (names.isEmpty()) {
                throw new ResourceNotFoundException("Category " + categoryId + " was not found");
            }
            return names.getFirst();
        }
        return "All products";
    }

    private Query salesQuery(String select, String extraJoins, ReportFilter filter) {
        StringBuilder sql = new StringBuilder(select)
                .append(SALES_FROM)
                .append(extraJoins)
                .append(" WHERE o.order_status <> 'CANCELLED'");
        List<Object> args = new ArrayList<>();
        filter.appendTo(sql, args);
        return new Query(sql.toString(), args.toArray());
    }

    private static void appendLocation(
            StringBuilder sql,
            List<Object> args,
            ReportFilter filter,
            String storeAlias) {
        if (filter.storeId() != null) {
            sql.append(" AND ").append(storeAlias).append(".store_id = ?");
            args.add(filter.storeId());
        }
        if (filter.regionId() != null) {
            sql.append(" AND ").append(storeAlias).append(".region_id = ?");
            args.add(filter.regionId());
        }
    }

    static BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal percent(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(whole, 1, RoundingMode.HALF_UP);
    }

    static BigDecimal average(BigDecimal total, long count) {
        if (count == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private static <T> BigDecimal sum(List<T> rows, Function<T, BigDecimal> value) {
        return rows.stream().map(value).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private record Query(String sql, Object[] args) {
    }
}
