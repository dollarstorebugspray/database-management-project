package com.grocerystore.backend.service;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grocerystore.backend.dto.CustomerRequest;
import com.grocerystore.backend.dto.CustomerResponse;
import com.grocerystore.backend.exception.BadRequestException;
import com.grocerystore.backend.exception.ResourceNotFoundException;
import com.grocerystore.backend.util.Ids;
import com.grocerystore.backend.util.Text;

@Service
public class CustomerService {

    private static final String SELECT_CUSTOMERS = """
            SELECT c.customer_id, c.name, c.customer_type,
                   c.street, c.city, c.state, c.zip_code,
                   h.marital_status, h.gender, h.age, h.income,
                   b.business_category, b.annual_income,
                   COALESCE(stats.order_count, 0) AS order_count,
                   COALESCE(stats.total_spent, 0) AS total_spent,
                   stats.last_order_date,
                   EXISTS (SELECT 1 FROM UserAccount a WHERE a.customer_id = c.customer_id) AS has_account
            FROM Customer c
            LEFT JOIN HomeCustomer h ON h.customer_id = c.customer_id
            LEFT JOIN BusinessCustomer b ON b.customer_id = c.customer_id
            LEFT JOIN (
                SELECT o.customer_id,
                       COUNT(DISTINCT o.order_id) AS order_count,
                       SUM(oi.quantity * oi.sale_price) AS total_spent,
                       MAX(o.order_date) AS last_order_date
                FROM Orders o
                JOIN OrderItem oi ON oi.order_id = o.order_id
                WHERE o.order_status <> 'CANCELLED'
                GROUP BY o.customer_id
            ) stats ON stats.customer_id = c.customer_id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<CustomerResponse> customerMapper = this::mapCustomer;

    public CustomerService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        return findAll("", null);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll(String search, String customerType) {
        StringBuilder sql = new StringBuilder(SELECT_CUSTOMERS).append(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        String text = Text.trimToEmpty(search);
        if (!text.isEmpty()) {
            sql.append("""
                     AND (c.name LIKE ? OR COALESCE(c.city, '') LIKE ?
                          OR COALESCE(b.business_category, '') LIKE ?)
                    """);
            String pattern = Text.likePattern(text);
            args.add(pattern);
            args.add(pattern);
            args.add(pattern);
        }
        String type = Text.trimToNull(customerType);
        if (type != null && !type.equalsIgnoreCase("ALL")) {
            sql.append(" AND c.customer_type = ?");
            args.add(type.toUpperCase(Locale.ROOT));
        }
        sql.append(" ORDER BY c.customer_id");
        return jdbcTemplate.query(sql.toString(), customerMapper, args.toArray());
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(Integer customerId) {
        List<CustomerResponse> customers = jdbcTemplate.query(
                SELECT_CUSTOMERS + " WHERE c.customer_id = ?",
                customerMapper,
                customerId);

        if (customers.isEmpty()) {
            throw new ResourceNotFoundException("Customer " + customerId + " was not found");
        }
        return customers.getFirst();
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        // Validate customer type and subtype information
        String customerType = normalizeAndValidate(request);
        int customerId = Ids.next(jdbcTemplate, "Customer", "customer_id");

        // Create the general customer
        jdbcTemplate.update("""
                INSERT INTO Customer
                    (customer_id, name, street, city, state, zip_code, customer_type)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                customerId,
                request.name().trim(),
                Text.trimToNull(request.street()),
                Text.trimToNull(request.city()),
                Text.trimToNull(request.state()),
                Text.trimToNull(request.zipCode()),
                customerType);

        // Create the appropriate customer subtype
        insertCustomerDetails(customerId, customerType, request);
        return findById(customerId);
    }

    @Transactional
    public CustomerResponse update(Integer customerId, CustomerRequest request) {
        // Find the existing customer
        findById(customerId);
        // Validate the request
        String customerType = normalizeAndValidate(request);

        // Update general customer information
        jdbcTemplate.update("""
                UPDATE Customer
                SET name = ?, street = ?, city = ?, state = ?, zip_code = ?, customer_type = ?
                WHERE customer_id = ?
                """,
                request.name().trim(),
                Text.trimToNull(request.street()),
                Text.trimToNull(request.city()),
                Text.trimToNull(request.state()),
                Text.trimToNull(request.zipCode()),
                customerType,
                customerId);

        jdbcTemplate.update("DELETE FROM HomeCustomer WHERE customer_id = ?", customerId);
        jdbcTemplate.update("DELETE FROM BusinessCustomer WHERE customer_id = ?", customerId);
        insertCustomerDetails(customerId, customerType, request);

        return findById(customerId);
    }

    @Transactional
    public void delete(Integer customerId) {
        // Find the customer
        findById(customerId);
        // Prevent deletion if the customer has existing orders
        Integer orders = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM Orders WHERE customer_id = ?",
                Integer.class,
                customerId);
        if (orders != null && orders > 0) {
            throw new BadRequestException("A customer with orders cannot be deleted");
        }
        jdbcTemplate.update("DELETE FROM UserAccount WHERE customer_id = ?", customerId);
        // Delete the appropriate customer subtype first
        jdbcTemplate.update("DELETE FROM HomeCustomer WHERE customer_id = ?", customerId);
        jdbcTemplate.update("DELETE FROM BusinessCustomer WHERE customer_id = ?", customerId);
        // Delete the general customer record
        jdbcTemplate.update("DELETE FROM Customer WHERE customer_id = ?", customerId);
    }

    private String normalizeAndValidate(CustomerRequest request) {
        String customerType = request.customerType().trim().toUpperCase(Locale.ROOT);
        if (customerType.equals("BUSINESS") && Text.trimToNull(request.businessCategory()) == null) {
            throw new BadRequestException("businessCategory is required for a business customer");
        }
        return customerType;
    }

    private void insertCustomerDetails(
            Integer customerId,
            String customerType,
            CustomerRequest request) {
        if (customerType.equals("HOME")) {
            jdbcTemplate.update("""
                    INSERT INTO HomeCustomer
                        (customer_id, marital_status, gender, age, income)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    customerId,
                    Text.trimToNull(request.maritalStatus()),
                    Text.trimToNull(request.gender()),
                    request.age(),
                    request.income());
            return;
        }

        jdbcTemplate.update("""
                INSERT INTO BusinessCustomer
                    (customer_id, business_category, annual_income)
                VALUES (?, ?, ?)
                """,
                customerId,
                request.businessCategory().trim(),
                request.annualIncome());
    }

    private CustomerResponse mapCustomer(ResultSet resultSet, int rowNumber) throws SQLException {
        Date lastOrder = resultSet.getDate("last_order_date");
        return new CustomerResponse(
                resultSet.getInt("customer_id"),
                resultSet.getString("name"),
                resultSet.getString("customer_type"),
                resultSet.getString("street"),
                resultSet.getString("city"),
                resultSet.getString("state"),
                resultSet.getString("zip_code"),
                resultSet.getString("marital_status"),
                resultSet.getString("gender"),
                resultSet.getObject("age", Integer.class),
                resultSet.getBigDecimal("income"),
                resultSet.getString("business_category"),
                resultSet.getBigDecimal("annual_income"),
                resultSet.getInt("order_count"),
                resultSet.getBigDecimal("total_spent"),
                lastOrder == null ? null : lastOrder.toLocalDate(),
                resultSet.getBoolean("has_account"));
    }
}
