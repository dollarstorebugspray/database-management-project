package com.grocerystore.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.grocerystore.backend.dto.OrderLineRequest;
import com.grocerystore.backend.exception.BadRequestException;

class OrderItemChangeTests {

    @Test
    void emptyRequestIsRejectedBeforeTouchingTheDatabase() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        OrderService service = new OrderService(jdbcTemplate);

        assertThatThrownBy(() -> service.cancelItems(1, List.of(), false))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Choose at least one product");
        assertThatThrownBy(() -> service.returnItems(1, List.of(), true))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void sameProductTwiceIsRejected() {
        assertThatThrownBy(() -> OrderService.requestedQuantities(List.of(
                new OrderLineRequest(3, 1), new OrderLineRequest(3, 2))))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Product 3 appears more than once");
    }

    @Test
    void requestIsSortedByProductId() {
        Map<Integer, Integer> requested = OrderService.requestedQuantities(List.of(
                new OrderLineRequest(9, 1), new OrderLineRequest(2, 4)));

        assertThat(requested.keySet()).containsExactly(2, 9);
    }

    @Test
    void remainingQuantitiesAreWorkedOutPerLine() {
        Map<Integer, Integer> remaining = OrderService.remainingAfter(
                Map.of(1, 5, 2, 1),
                Map.of(1, 2, 2, 1),
                Map.of());

        assertThat(remaining).containsEntry(1, 3).containsEntry(2, 0);
    }

    @Test
    void cannotRemoveMoreThanWasOrdered() {
        assertThatThrownBy(() -> OrderService.remainingAfter(
                Map.of(1, 2), Map.of(1, 3), Map.of(1, "Whole Milk")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only 2 of Whole Milk are on this order");
    }

    @Test
    void cannotRemoveAProductThatIsNotOnTheOrder() {
        assertThatThrownBy(() -> OrderService.remainingAfter(Map.of(1, 2), Map.of(7, 1), Map.of()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Product 7 is not on this order");
    }

    @Test
    void returnsAreAcceptedForThirtyDays() {
        LocalDate today = LocalDate.of(2026, 11, 22);

        assertThatCode(() -> OrderService.requireReturnWindow(today.minusDays(30), today))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> OrderService.requireReturnWindow(today.minusDays(31), today))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Returns are accepted within 30 days of the order date");
    }
}
