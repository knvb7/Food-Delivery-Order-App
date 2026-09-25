package com.dmg.fooddelivery.model;

import com.dmg.fooddelivery.common.ApiException;

public enum OrderStatus {
    PLACED,
    ACCEPTED,
    PREPARING,
    OUT_FOR_DELIVERY,
    DELIVERED,
    REJECTED,
    CANCELLED;

    public void requireTransitionTo(OrderStatus next) {
        boolean allowed =
                switch (this) {
                    case PLACED -> next == ACCEPTED || next == REJECTED || next == CANCELLED;
                    case ACCEPTED -> next == PREPARING;
                    case PREPARING -> next == OUT_FOR_DELIVERY;
                    case OUT_FOR_DELIVERY -> next == DELIVERED;
                    default -> false;
                };
        if (!allowed) {
            throw ApiException.conflict("Cannot change order from " + this + " to " + next);
        }
    }

    public Role requiredRole() {
        return switch (this) {
            case ACCEPTED, PREPARING, REJECTED -> Role.OWNER;
            case OUT_FOR_DELIVERY, DELIVERED -> Role.PARTNER;
            case CANCELLED -> Role.CUSTOMER;
            default -> throw ApiException.badRequest("Cannot transition to PLACED");
        };
    }

    public boolean isAssignable() {
        return this == ACCEPTED || this == PREPARING;
    }
}
