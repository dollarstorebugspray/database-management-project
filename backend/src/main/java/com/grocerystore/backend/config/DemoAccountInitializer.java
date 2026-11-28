package com.grocerystore.backend.config;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.grocerystore.backend.util.Ids;

@Component
public class DemoAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoAccountInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final boolean demoAccountsEnabled;
    private final String salespersonEmail;
    private final String salespersonPassword;
    private final int salespersonId;
    private final String customerEmail;
    private final String customerPassword;
    private final int customerId;
    private final String businessEmail;
    private final String businessPassword;
    private final int businessCustomerId;

    public DemoAccountInitializer(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            @Value("${app.demo.admin-email}") String adminEmail,
            @Value("${app.demo.admin-password}") String adminPassword,
            @Value("${app.demo.accounts-enabled:true}") boolean demoAccountsEnabled,
            @Value("${app.demo.salesperson-email:salesperson@grocerystore.local}") String salespersonEmail,
            @Value("${app.demo.salesperson-password:Salesperson123!}") String salespersonPassword,
            @Value("${app.demo.salesperson-id:2}") int salespersonId,
            @Value("${app.demo.customer-email:customer@grocerystore.local}") String customerEmail,
            @Value("${app.demo.customer-password:Customer123!}") String customerPassword,
            @Value("${app.demo.customer-id:1}") int customerId,
            @Value("${app.demo.business-email:business@grocerystore.local}") String businessEmail,
            @Value("${app.demo.business-password:Business123!}") String businessPassword,
            @Value("${app.demo.business-customer-id:2}") int businessCustomerId) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.demoAccountsEnabled = demoAccountsEnabled;
        this.salespersonEmail = salespersonEmail;
        this.salespersonPassword = salespersonPassword;
        this.salespersonId = salespersonId;
        this.customerEmail = customerEmail;
        this.customerPassword = customerPassword;
        this.customerId = customerId;
        this.businessEmail = businessEmail;
        this.businessPassword = businessPassword;
        this.businessCustomerId = businessCustomerId;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        createAccount(adminEmail, adminPassword, "ADMIN", null, null);
        if (!demoAccountsEnabled) {
            return;
        }
        if (exists("Salesperson", "salesperson_id", salespersonId)
                && !exists("UserAccount", "salesperson_id", salespersonId)) {
            createAccount(salespersonEmail, salespersonPassword, "EMPLOYEE", null, salespersonId);
        }
        if (exists("Customer", "customer_id", customerId)
                && !exists("UserAccount", "customer_id", customerId)) {
            createAccount(customerEmail, customerPassword, "CUSTOMER", customerId, null);
        }
        if (exists("Customer", "customer_id", businessCustomerId)
                && !exists("UserAccount", "customer_id", businessCustomerId)) {
            createAccount(businessEmail, businessPassword, "CUSTOMER", businessCustomerId, null);
        }
    }

    private void createAccount(
            String rawEmail,
            String password,
            String role,
            Integer linkedCustomerId,
            Integer linkedSalespersonId) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM UserAccount WHERE email = ?",
                Integer.class,
                email);
        if (count != null && count > 0) {
            return;
        }

        int accountId = Ids.next(jdbcTemplate, "UserAccount", "account_id");
        jdbcTemplate.update("""
                INSERT INTO UserAccount
                    (account_id, email, password_hash, role, customer_id, salesperson_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                accountId,
                email,
                passwordEncoder.encode(password),
                role,
                linkedCustomerId,
                linkedSalespersonId);
        log.info("Created demo {} account {}", role, email);
    }

    private boolean exists(String table, String column, int id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?",
                Integer.class,
                id);
        return count != null && count > 0;
    }
}
