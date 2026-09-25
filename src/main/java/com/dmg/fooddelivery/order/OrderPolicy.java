package com.dmg.fooddelivery.order;

import static com.dmg.fooddelivery.order.OrderStatus.*;
import com.dmg.fooddelivery.common.ApiException;
import com.dmg.fooddelivery.security.Role;

public final class OrderPolicy {
    private OrderPolicy() {}

    public static void requireRole(Role role, OrderStatus target) {
        boolean allowed = switch (target) {
            case ACCEPTED, PREPARING, REJECTED -> role == Role.OWNER;
            case OUT_FOR_DELIVERY, DELIVERED -> role == Role.PARTNER;
            case CANCELLED -> role == Role.CUSTOMER;
            default -> false;
        };
        if (!allowed) throw ApiException.forbidden();
    }

    public static void requireTransition(OrderStatus from, OrderStatus to) {
        boolean allowed = switch (from) {
            case PLACED -> to == ACCEPTED || to == REJECTED || to == CANCELLED;
            case ACCEPTED -> to == PREPARING;
            case PREPARING -> to == OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> to == DELIVERED;
            default -> false;
        };
        if (!allowed) throw ApiException.conflict("Cannot change order from " + from + " to " + to);
    }

    public static boolean assignable(OrderStatus status) { return status == ACCEPTED || status == PREPARING; }
}
