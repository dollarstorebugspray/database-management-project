package com.grocerystore.backend.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.grocerystore.backend.exception.BadRequestException;

class OrderStatusTests {

    @Test
    void ordersMoveForward() {
        assertThatCode(() -> OrderStatus.PENDING.requireTransitionTo(OrderStatus.PAID)).doesNotThrowAnyException();
        assertThatCode(() -> OrderStatus.PAID.requireTransitionTo(OrderStatus.PROCESSING)).doesNotThrowAnyException();
        assertThatCode(() -> OrderStatus.PROCESSING.requireTransitionTo(OrderStatus.COMPLETED)).doesNotThrowAnyException();
        assertThatCode(() -> OrderStatus.PAID.requireTransitionTo(OrderStatus.COMPLETED)).doesNotThrowAnyException();
    }

    @Test
    void unpaidOrdersMustBePaidFirst() {
        assertThatThrownBy(() -> OrderStatus.PENDING.requireTransitionTo(OrderStatus.COMPLETED))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("This order has not been paid yet. Mark it PAID before moving it to COMPLETED");
        assertThatThrownBy(() -> OrderStatus.PENDING.requireTransitionTo(OrderStatus.PROCESSING))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("This order has not been paid yet. Mark it PAID before moving it to PROCESSING");
        assertThatCode(() -> OrderStatus.PENDING.requireTransitionTo(OrderStatus.CANCELLED))
                .doesNotThrowAnyException();
    }

    @Test
    void paidMeansMoneyWasReceived() {
        assertThat(OrderStatus.PENDING.isPaid()).isFalse();
        assertThat(OrderStatus.PAID.isPaid()).isTrue();
        assertThat(OrderStatus.PROCESSING.isPaid()).isTrue();
        assertThat(OrderStatus.COMPLETED.isPaid()).isTrue();
        assertThat(OrderStatus.CANCELLED.isPaid()).isFalse();
    }

    @Test
    void ordersCannotMoveBackward() {
        assertThatThrownBy(() -> OrderStatus.PROCESSING.requireTransitionTo(OrderStatus.PAID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("An order cannot move back from PROCESSING to PAID");
    }

    @Test
    void openOrdersCanBeCancelled() {
        assertThatCode(() -> OrderStatus.PROCESSING.requireTransitionTo(OrderStatus.CANCELLED))
                .doesNotThrowAnyException();
    }

    @Test
    void finalStatesCannotChange() {
        assertThatThrownBy(() -> OrderStatus.CANCELLED.requireTransitionTo(OrderStatus.PENDING))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("A cancelled order cannot be reopened");
        assertThatThrownBy(() -> OrderStatus.COMPLETED.requireTransitionTo(OrderStatus.CANCELLED))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("A completed order cannot be changed");
    }

    @Test
    void keepingTheSameStatusIsAllowed() {
        assertThatCode(() -> OrderStatus.CANCELLED.requireTransitionTo(OrderStatus.CANCELLED))
                .doesNotThrowAnyException();
    }

    @Test
    void customersCanCancelOnlyBeforePreparation() {
        assertThat(OrderStatus.PENDING.customerCanCancel()).isTrue();
        assertThat(OrderStatus.PAID.customerCanCancel()).isTrue();
        assertThat(OrderStatus.PROCESSING.customerCanCancel()).isFalse();
        assertThat(OrderStatus.COMPLETED.customerCanCancel()).isFalse();
    }

    @Test
    void parseRejectsUnknownStatus() {
        assertThat(OrderStatus.parse(" paid ")).isEqualTo(OrderStatus.PAID);
        assertThatThrownBy(() -> OrderStatus.parse("SHIPPED"))
                .isInstanceOf(BadRequestException.class);
    }
}
