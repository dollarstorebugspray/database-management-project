package com.grocerystore.backend.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grocerystore.backend.dto.AccountRequest;
import com.grocerystore.backend.dto.AccountResponse;
import com.grocerystore.backend.exception.BadRequestException;
import com.grocerystore.backend.exception.ResourceNotFoundException;
import com.grocerystore.backend.util.Ids;

@Service
public class AccountService {

    private static final String SELECT_ACCOUNTS = """
            SELECT a.account_id, a.email, a.role, a.customer_id, a.salesperson_id,
                   COALESCE(c.name, e.name) AS name, a.created_at
            FROM UserAccount a
            LEFT JOIN Customer c ON c.customer_id = a.customer_id
            LEFT JOIN Salesperson e ON e.salesperson_id = a.salesperson_id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public AccountService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> findStaffAccounts() {
        return jdbcTemplate.query(
                SELECT_ACCOUNTS + " WHERE a.role IN ('EMPLOYEE', 'ADMIN') ORDER BY a.role, a.email",
                (resultSet, rowNumber) -> {
                    Timestamp created = resultSet.getTimestamp("created_at");
                    return new AccountResponse(
                            resultSet.getInt("account_id"),
                            resultSet.getString("email"),
                            resultSet.getString("role"),
                            resultSet.getObject("customer_id", Integer.class),
                            resultSet.getObject("salesperson_id", Integer.class),
                            resultSet.getString("name"),
                            created == null ? null : created.toLocalDateTime());
                });
    }

    @Transactional
    public AccountResponse createStaffAccount(AccountRequest request) {
        String email = AuthService.normalizeEmail(request.email());
        String role = request.role().trim().toUpperCase(Locale.ROOT);

        if (role.equals("EMPLOYEE") && request.salespersonId() == null) {
            throw new BadRequestException("salespersonId is required for an salesperson account");
        }
        if (request.salespersonId() != null) {
            Integer salespeople = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM Salesperson WHERE salesperson_id = ?",
                    Integer.class,
                    request.salespersonId());
            if (salespeople == null || salespeople == 0) {
                throw new ResourceNotFoundException("Salesperson " + request.salespersonId() + " was not found");
            }
            Integer linked = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM UserAccount WHERE salesperson_id = ?",
                    Integer.class,
                    request.salespersonId());
            if (linked != null && linked > 0) {
                throw new BadRequestException("This salesperson already has a login account");
            }
        }
        Integer emailCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM UserAccount WHERE email = ?",
                Integer.class,
                email);
        if (emailCount != null && emailCount > 0) {
            throw new BadRequestException("An account with this email already exists");
        }

        int accountId = Ids.next(jdbcTemplate, "UserAccount", "account_id");
        jdbcTemplate.update("""
                INSERT INTO UserAccount (account_id, email, password_hash, role, salesperson_id)
                VALUES (?, ?, ?, ?, ?)
                """,
                accountId,
                email,
                passwordEncoder.encode(request.password()),
                role,
                request.salespersonId());

        return findStaffAccounts().stream()
                .filter(account -> account.accountId() == accountId)
                .findFirst()
                .orElseThrow();
    }
}
