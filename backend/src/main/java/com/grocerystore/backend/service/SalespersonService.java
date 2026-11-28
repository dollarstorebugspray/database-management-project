package com.grocerystore.backend.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grocerystore.backend.dto.SalespersonRequest;
import com.grocerystore.backend.dto.SalespersonResponse;
import com.grocerystore.backend.exception.ResourceNotFoundException;
import com.grocerystore.backend.util.Ids;
import com.grocerystore.backend.util.Text;

@Service
public class SalespersonService {

    private static final String SELECT_EMPLOYEES = """
            SELECT e.salesperson_id, e.name, e.street, e.city, e.state, e.zip_code,
                   e.email, e.job_title, e.salary, e.store_id,
                   s.city AS store_city, a.email AS account_email
            FROM Salesperson e
            JOIN Store s ON s.store_id = e.store_id
            LEFT JOIN UserAccount a ON a.salesperson_id = e.salesperson_id
            """;

    private final JdbcTemplate jdbcTemplate;

    public SalespersonService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<SalespersonResponse> findByStore(Integer storeId) {
        if (storeId == null) {
            return jdbcTemplate.query(
                    SELECT_EMPLOYEES + " ORDER BY e.store_id, e.name",
                    this::mapSalesperson);
        }
        requireStore(storeId);
        return jdbcTemplate.query(
                SELECT_EMPLOYEES + " WHERE e.store_id = ? ORDER BY e.name",
                this::mapSalesperson,
                storeId);
    }

    @Transactional
    public SalespersonResponse create(SalespersonRequest request) {
        requireStore(request.storeId());
        int salespersonId = Ids.next(jdbcTemplate, "Salesperson", "salesperson_id");

        jdbcTemplate.update("""
                INSERT INTO Salesperson
                    (salesperson_id, name, street, city, state, zip_code,
                     email, job_title, salary, store_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                salespersonId,
                request.name().trim(),
                Text.trimToNull(request.street()),
                Text.trimToNull(request.city()),
                Text.trimToNull(request.state()),
                Text.trimToNull(request.zipCode()),
                Text.trimToNull(request.email()),
                request.jobTitle().trim(),
                request.salary(),
                request.storeId());
        return findOne(salespersonId);
    }

    @Transactional
    public SalespersonResponse update(Integer salespersonId, SalespersonRequest request) {
        findOne(salespersonId);
        requireStore(request.storeId());

        jdbcTemplate.update("""
                UPDATE Salesperson
                SET name = ?, street = ?, city = ?, state = ?, zip_code = ?,
                    email = ?, job_title = ?, salary = ?, store_id = ?
                WHERE salesperson_id = ?
                """,
                request.name().trim(),
                Text.trimToNull(request.street()),
                Text.trimToNull(request.city()),
                Text.trimToNull(request.state()),
                Text.trimToNull(request.zipCode()),
                Text.trimToNull(request.email()),
                request.jobTitle().trim(),
                request.salary(),
                request.storeId(),
                salespersonId);
        return findOne(salespersonId);
    }

    private SalespersonResponse findOne(Integer salespersonId) {
        List<SalespersonResponse> salespeople = jdbcTemplate.query(
                SELECT_EMPLOYEES + " WHERE e.salesperson_id = ?",
                this::mapSalesperson,
                salespersonId);
        if (salespeople.isEmpty()) {
            throw new ResourceNotFoundException("Salesperson " + salespersonId + " was not found");
        }
        return salespeople.getFirst();
    }

    private void requireStore(Integer storeId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM Store WHERE store_id = ?",
                Integer.class,
                storeId);
        if (count == null || count == 0) {
            throw new ResourceNotFoundException("Store " + storeId + " was not found");
        }
    }

    private SalespersonResponse mapSalesperson(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SalespersonResponse(
                resultSet.getInt("salesperson_id"),
                resultSet.getString("name"),
                resultSet.getString("street"),
                resultSet.getString("city"),
                resultSet.getString("state"),
                resultSet.getString("zip_code"),
                resultSet.getString("email"),
                resultSet.getString("job_title"),
                resultSet.getBigDecimal("salary"),
                resultSet.getInt("store_id"),
                resultSet.getString("store_city"),
                resultSet.getString("account_email"));
    }
}
