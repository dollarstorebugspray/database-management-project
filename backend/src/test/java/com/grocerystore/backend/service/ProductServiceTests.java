package com.grocerystore.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.grocerystore.backend.dto.ProductSearch;
import com.grocerystore.backend.exception.BadRequestException;

class ProductServiceTests {

    @Test
    void searchRejectsAnInvertedPriceRangeBeforeQuerying() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ProductService service = new ProductService(jdbcTemplate, new ProductImages(List.of()));

        assertThatThrownBy(() -> service.search(new ProductSearch(
                1, "", null, new BigDecimal("10.00"), new BigDecimal("2.00"), false, "name", null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("minPrice cannot be greater than maxPrice");

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void searchRejectsAnUnknownSortBeforeQuerying() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ProductService service = new ProductService(jdbcTemplate, new ProductImages(List.of()));

        assertThatThrownBy(() -> service.search(new ProductSearch(
                1, "", null, null, null, false, "random", null)))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void sortDefaultsToNameAndIgnoresCase() {
        assertThat(ProductService.normalizeSort(null)).isEqualTo("name");
        assertThat(ProductService.normalizeSort(" PRICE_DESC ")).isEqualTo("price_desc");
    }
}
