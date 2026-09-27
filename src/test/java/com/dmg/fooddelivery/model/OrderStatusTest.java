package com.dmg.fooddelivery.model;

import com.dmg.fooddelivery.common.ApiException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusTest {

    @ParameterizedTest
    @MethodSource("allowedTransitions")
    void acceptsAllowedTransitions(OrderStatus current, OrderStatus next) {
        assertDoesNotThrow(() -> current.requireTransitionTo(next));
    }

    @ParameterizedTest
    @MethodSource("invalidTransitions")
    void rejectsInvalidTransitions(OrderStatus current, OrderStatus next) {
        ApiException exception =
                assertThrows(ApiException.class, () -> current.requireTransitionTo(next));

        assertEquals(409, exception.status().value());
        assertEquals("CONFLICT", exception.code());
        assertEquals("Cannot change order from " + current + " to " + next, exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("requiredRoles")
    void returnsRoleRequiredForEachTransitionTarget(OrderStatus status, Role role) {
        assertEquals(role, status.requiredRole());
    }

    @Test
    void placedCannotBeUsedAsTransitionTarget() {
        ApiException exception = assertThrows(ApiException.class, OrderStatus.PLACED::requiredRole);

        assertEquals(400, exception.status().value());
        assertEquals("INVALID_REQUEST", exception.code());
    }

    @Test
    void onlyAcceptedAndPreparingOrdersAreAssignable() {
        assertTrue(OrderStatus.ACCEPTED.isAssignable());
        assertTrue(OrderStatus.PREPARING.isAssignable());
        assertFalse(OrderStatus.PLACED.isAssignable());
        assertFalse(OrderStatus.OUT_FOR_DELIVERY.isAssignable());
        assertFalse(OrderStatus.DELIVERED.isAssignable());
        assertFalse(OrderStatus.REJECTED.isAssignable());
        assertFalse(OrderStatus.CANCELLED.isAssignable());
    }

    private static Stream<Arguments> allowedTransitions() {
        return Stream.of(
                Arguments.of(OrderStatus.PLACED, OrderStatus.ACCEPTED),
                Arguments.of(OrderStatus.PLACED, OrderStatus.REJECTED),
                Arguments.of(OrderStatus.PLACED, OrderStatus.CANCELLED),
                Arguments.of(OrderStatus.ACCEPTED, OrderStatus.PREPARING),
                Arguments.of(OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY),
                Arguments.of(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED));
    }

    private static Stream<Arguments> invalidTransitions() {
        return Stream.of(OrderStatus.values())
                .flatMap(current -> Stream.of(OrderStatus.values()).map(next -> Arguments.of(current, next)))
                .filter(
                        arguments -> {
                            OrderStatus current = (OrderStatus) arguments.get()[0];
                            OrderStatus next = (OrderStatus) arguments.get()[1];
                            return !isAllowed(current, next);
                        });
    }

    private static boolean isAllowed(OrderStatus current, OrderStatus next) {
        return (current == OrderStatus.PLACED
                        && (next == OrderStatus.ACCEPTED
                                || next == OrderStatus.REJECTED
                                || next == OrderStatus.CANCELLED))
                || (current == OrderStatus.ACCEPTED && next == OrderStatus.PREPARING)
                || (current == OrderStatus.PREPARING && next == OrderStatus.OUT_FOR_DELIVERY)
                || (current == OrderStatus.OUT_FOR_DELIVERY && next == OrderStatus.DELIVERED);
    }

    private static Stream<Arguments> requiredRoles() {
        return Stream.of(
                Arguments.of(OrderStatus.ACCEPTED, Role.OWNER),
                Arguments.of(OrderStatus.PREPARING, Role.OWNER),
                Arguments.of(OrderStatus.REJECTED, Role.OWNER),
                Arguments.of(OrderStatus.OUT_FOR_DELIVERY, Role.PARTNER),
                Arguments.of(OrderStatus.DELIVERED, Role.PARTNER),
                Arguments.of(OrderStatus.CANCELLED, Role.CUSTOMER));
    }
}
