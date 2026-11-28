package com.grocerystore.backend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.grocerystore.backend.dto.SalespersonRequest;
import com.grocerystore.backend.exception.ResourceNotFoundException;

class SalespersonServiceTests {

    @Test
    void createRejectsUnknownStore() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM Store WHERE store_id = ?",
                Integer.class,
                99)).thenReturn(0);

        SalespersonService service = new SalespersonService(jdbcTemplate);
        SalespersonRequest request = new SalespersonRequest(
                "Sam Rivera",
                null,
                null,
                null,
                null,
                "sam@example.com",
                "Salesperson",
                new BigDecimal("42000.00"),
                99);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Store 99 was not found");
    }
}
