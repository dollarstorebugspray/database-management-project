package com.grocerystore.backend.model;

import java.util.Locale;

import com.grocerystore.backend.exception.BadRequestException;

public enum OrderStatus {
    PENDING,
    PAID,
    PROCESSING,
    COMPLETED,
    CANCELLED;

    public static OrderStatus parse(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("status is required");
        }
        try {
            return OrderStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(
                    "status must be one of PENDING, PAID, PROCESSING, COMPLETED, CANCELLED");
        }
    }

    public void requireTransitionTo(OrderStatus next) {
        if (this == next) {
            return;
        }
        if (this == CANCELLED) {
            throw new BadRequestException("A cancelled order cannot be reopened");
        }
        if (this == COMPLETED) {
            throw new BadRequestException("A completed order cannot be changed");
        }
        if (next == CANCELLED) {
            return;
        }
        if (next.ordinal() < ordinal()) {
            throw new BadRequestException(
                    "An order cannot move back from " + this + " to " + next);
        }
        if (this == PENDING && next != PAID) {
            throw new BadRequestException(
                    "This order has not been paid yet. Mark it PAID before moving it to " + next);
        }
    }

    public boolean isPaid() {
        return this == PAID || this == PROCESSING || this == COMPLETED;
    }

    public boolean customerCanCancel() {
        return this == PENDING || this == PAID;
    }
}
