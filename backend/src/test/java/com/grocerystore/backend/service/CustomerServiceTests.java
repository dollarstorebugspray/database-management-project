package com.grocerystore.backend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.grocerystore.backend.dto.CustomerRequest;
import com.grocerystore.backend.exception.BadRequestException;

class CustomerServiceTests {

    @Test
    void createBusinessCustomerRequiresBusinessCategory() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        CustomerService service = new CustomerService(jdbcTemplate);
        CustomerRequest request = new CustomerRequest(
                "Steel City Cafe",
                "BUSINESS",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("businessCategory is required for a business customer");

        verifyNoInteractions(jdbcTemplate);
    }
}
