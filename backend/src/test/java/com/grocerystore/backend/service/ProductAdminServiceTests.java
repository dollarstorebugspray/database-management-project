package com.grocerystore.backend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.grocerystore.backend.dto.ProductWriteRequest;
import com.grocerystore.backend.exception.BadRequestException;
import com.grocerystore.backend.exception.ResourceNotFoundException;

class ProductAdminServiceTests {

    @Test
    void updateRejectsUnknownProduct() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM Product WHERE product_id = ?",
                Integer.class,
                99)).thenReturn(0);

        ProductAdminService service = new ProductAdminService(jdbcTemplate, new ProductImages(List.of()));
        ProductWriteRequest request = new ProductWriteRequest(
                "Orange Juice",
                "Grocery Store",
                new BigDecimal("4.79"),
                1,
                1,
                24,
                new BigDecimal("3.10"),
                null);

        assertThatThrownBy(() -> service.update(99, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product 99 was not found");
    }

    @Test
    void newProductWithStockNeedsAUnitCost() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ProductAdminService service = new ProductAdminService(jdbcTemplate, new ProductImages(List.of()));
        ProductWriteRequest request = new ProductWriteRequest(
                "Orange Juice", "Grocery Store", new BigDecimal("4.79"), 1, 1, 24, null, null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("unitCost is required when the product starts with stock");

        verifyNoInteractions(jdbcTemplate);
    }
}
