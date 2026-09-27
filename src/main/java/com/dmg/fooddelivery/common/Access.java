package com.dmg.fooddelivery.common;

import com.dmg.fooddelivery.model.CustomerOrder;
import com.dmg.fooddelivery.model.Restaurant;
import com.dmg.fooddelivery.model.Role;
import com.dmg.fooddelivery.model.User;

public final class Access {

    private Access() {}

    public static void requireRole(User user, Role role) {
        if (user.getRole() != role) {
            throw ApiException.forbidden();
        }
    }

    public static void requireOwner(User user, Restaurant restaurant) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }

        requireRole(user, Role.OWNER);
        if (!user.getId().equals(restaurant.getOwner().getId())) {
            throw ApiException.forbidden();
        }
    }

    public static void requireOrderAccess(User user, CustomerOrder order) {
        boolean allowed =
                switch (user.getRole()) {
                    case ADMIN -> true;
                    case CUSTOMER -> user.getId().equals(order.getCustomer().getId());
                    case OWNER -> user.getId().equals(order.getRestaurant().getOwner().getId());
                    case PARTNER -> order.getPartner() != null && user.getId().equals(order.getPartner().getUser().getId());
                };
        if (!allowed) {
            throw ApiException.forbidden();
        }
    }
}
