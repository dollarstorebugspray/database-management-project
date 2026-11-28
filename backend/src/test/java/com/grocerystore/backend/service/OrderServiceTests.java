package com.grocerystore.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.grocerystore.backend.dto.CreateOrderRequest;
import com.grocerystore.backend.dto.OrderLineRequest;
import com.grocerystore.backend.exception.BadRequestException;
import com.grocerystore.backend.model.OrderStatus;

class OrderServiceTests {

    @Test
    void createRejectsDuplicateProductsBeforeChangingTheDatabase() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        OrderService service = new OrderService(jdbcTemplate);
        CreateOrderRequest request = new CreateOrderRequest(
                1,
                1,
                "Credit Card",
                List.of(
                        new OrderLineRequest(1, 2),
                        new OrderLineRequest(1, 1)));

        assertThatThrownBy(() -> service.create(request, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Product 1 appears more than once in the order");

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void createRejectsAnEmptyOrderBeforeChangingTheDatabase() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        OrderService service = new OrderService(jdbcTemplate);
        CreateOrderRequest request = new CreateOrderRequest(
                1,
                1,
                "Credit Card",
                List.of());

        assertThatThrownBy(() -> service.create(request, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("An order must contain at least one product");

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void createRejectsAnUnknownPaymentMethodBeforeChangingTheDatabase() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        OrderService service = new OrderService(jdbcTemplate);
        CreateOrderRequest request = new CreateOrderRequest(
                1,
                1,
                "Gift Card",
                List.of(new OrderLineRequest(1, 1)));

        assertThatThrownBy(() -> service.create(request, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageStartingWith("paymentMethod must be one of");

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void paymentMethodIsMatchedWithoutRegardToCase() {
        assertThat(OrderService.normalizePaymentMethod(" debit card ")).isEqualTo("Debit Card");
        assertThat(OrderService.normalizePaymentMethod("business invoice")).isEqualTo("Business Invoice");
    }

    @Test
    void storeWithoutStaffCannotTakeOrders() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), eq(Integer.class), any(), any())).thenReturn(List.of());
        OrderService service = new OrderService(jdbcTemplate);

        assertThatThrownBy(() -> service.chooseSalesperson(90, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Store 90 has no staff yet, so it can't take orders. Please choose another store.");
    }

    @Test
    void onlineOrderGoesToTheFirstCandidate() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), eq(Integer.class), any(), any())).thenReturn(List.of(7, 3));
        OrderService service = new OrderService(jdbcTemplate);

        assertThat(service.chooseSalesperson(1, null)).isEqualTo(7);
    }

    @Test
    void cardPaymentsArePaidAtCheckoutAndOthersStartPending() {
        assertThat(OrderService.initialStatus("Credit Card")).isEqualTo(OrderStatus.PAID);
        assertThat(OrderService.initialStatus("Debit Card")).isEqualTo(OrderStatus.PAID);
        assertThat(OrderService.initialStatus("Cash at Pickup")).isEqualTo(OrderStatus.PENDING);
        assertThat(OrderService.initialStatus("Business Invoice")).isEqualTo(OrderStatus.PENDING);
    }
}
