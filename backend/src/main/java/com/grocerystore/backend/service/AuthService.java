package com.grocerystore.backend.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import jakarta.servlet.http.HttpSession;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grocerystore.backend.dto.AuthResponse;
import com.grocerystore.backend.dto.CustomerRequest;
import com.grocerystore.backend.dto.CustomerResponse;
import com.grocerystore.backend.dto.LoginRequest;
import com.grocerystore.backend.dto.RegisterRequest;
import com.grocerystore.backend.exception.BadRequestException;
import com.grocerystore.backend.exception.ForbiddenException;
import com.grocerystore.backend.exception.UnauthorizedException;
import com.grocerystore.backend.model.SessionUser;
import com.grocerystore.backend.util.Ids;

@Service
public class AuthService {

    private static final String SESSION_USER = "sessionUser";

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final CustomerService customerService;

    public AuthService(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            CustomerService customerService) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.customerService = customerService;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request, HttpSession session) {
        String email = normalizeEmail(request.email());
        List<AccountRecord> accounts = jdbcTemplate.query("""
                SELECT a.account_id, a.email, a.password_hash, a.role,
                       a.customer_id, a.salesperson_id,
                       COALESCE(c.name, e.name) AS display_name,
                       e.store_id
                FROM UserAccount a
                LEFT JOIN Customer c ON c.customer_id = a.customer_id
                LEFT JOIN Salesperson e ON e.salesperson_id = a.salesperson_id
                WHERE a.email = ?
                """,
                (resultSet, rowNumber) -> new AccountRecord(
                        resultSet.getInt("account_id"),
                        resultSet.getString("email"),
                        resultSet.getString("password_hash"),
                        resultSet.getString("role"),
                        resultSet.getObject("customer_id", Integer.class),
                        resultSet.getObject("salesperson_id", Integer.class),
                        resultSet.getString("display_name"),
                        resultSet.getObject("store_id", Integer.class)),
                email);

        if (accounts.isEmpty() || !passwordEncoder.matches(
                request.password(), accounts.getFirst().passwordHash())) {
            throw new UnauthorizedException("The email or password is incorrect");
        }

        AccountRecord account = accounts.getFirst();
        String displayName = account.displayName() != null
                ? account.displayName()
                : (account.role().equals("ADMIN") ? "Administrator" : account.email());
        SessionUser user = new SessionUser(
                account.accountId(),
                account.email(),
                account.role(),
                account.customerId(),
                account.salespersonId(),
                displayName,
                account.storeId());
        session.setAttribute(SESSION_USER, user);
        return AuthResponse.from(user);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpSession session) {
        String email = normalizeEmail(request.email());
        if (emailExists(email)) {
            throw new BadRequestException("An account with this email already exists");
        }

        String customerType = request.customerType() == null
                ? "HOME"
                : request.customerType().trim().toUpperCase(Locale.ROOT);
        CustomerResponse customer = customerService.create(new CustomerRequest(
                request.name(),
                customerType,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                request.businessCategory(),
                null));

        int accountId = Ids.next(jdbcTemplate, "UserAccount", "account_id");
        jdbcTemplate.update("""
                INSERT INTO UserAccount
                    (account_id, email, password_hash, role, customer_id)
                VALUES (?, ?, ?, 'CUSTOMER', ?)
                """,
                accountId,
                email,
                passwordEncoder.encode(request.password()),
                customer.customerId());

        SessionUser user = new SessionUser(
                accountId, email, "CUSTOMER", customer.customerId(), null, customer.name(), null);
        session.setAttribute(SESSION_USER, user);
        return AuthResponse.from(user);
    }

    public AuthResponse currentUser(HttpSession session) {
        return AuthResponse.from(requireUser(session));
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    public void refreshDisplayName(HttpSession session, Integer customerId, String name) {
        optionalUser(session)
                .filter(user -> customerId.equals(user.customerId()))
                .ifPresent(user -> session.setAttribute(SESSION_USER, new SessionUser(
                        user.accountId(),
                        user.email(),
                        user.role(),
                        user.customerId(),
                        user.salespersonId(),
                        name,
                        user.storeId())));
    }

    public Optional<SessionUser> optionalUser(HttpSession session) {
        Object value = session.getAttribute(SESSION_USER);
        if (value instanceof SessionUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public SessionUser requireUser(HttpSession session) {
        return optionalUser(session)
                .orElseThrow(() -> new UnauthorizedException("Please sign in to continue"));
    }

    public SessionUser requireStaff(HttpSession session) {
        SessionUser user = requireUser(session);
        if (!user.isStaff()) {
            throw new ForbiddenException("Staff access is required");
        }
        return user;
    }

    public SessionUser requireAdmin(HttpSession session) {
        SessionUser user = requireUser(session);
        if (!user.isAdmin()) {
            throw new ForbiddenException("Administrator access is required");
        }
        return user;
    }

    public SessionUser requireCustomerAccess(Integer customerId, HttpSession session) {
        SessionUser user = requireUser(session);
        if (user.isStaff()) {
            return user;
        }
        if (user.customerId() == null || !user.customerId().equals(customerId)) {
            throw new ForbiddenException("You cannot access another customer's account");
        }
        return user;
    }

    boolean emailExists(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM UserAccount WHERE email = ?",
                Integer.class,
                email);
        return count != null && count > 0;
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private record AccountRecord(
            Integer accountId,
            String email,
            String passwordHash,
            String role,
            Integer customerId,
            Integer salespersonId,
            String displayName,
            Integer storeId) {
    }
}
