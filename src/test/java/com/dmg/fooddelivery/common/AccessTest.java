package com.dmg.fooddelivery.common;

import com.dmg.fooddelivery.model.CustomerOrder;
import com.dmg.fooddelivery.model.DeliveryPartner;
import com.dmg.fooddelivery.model.Restaurant;
import com.dmg.fooddelivery.model.Role;
import com.dmg.fooddelivery.model.User;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccessTest {

    @Test
    void requireRoleAllowsOnlyRequestedRole() {
        User customer = user(1, Role.CUSTOMER);
        assertDoesNotThrow(() -> Access.requireRole(customer, Role.CUSTOMER));

        assertForbidden(() -> Access.requireRole(customer, Role.ADMIN));
    }

    @Test
    void adminAndRestaurantOwnerCanManageRestaurant() {
        User admin = user(1, Role.ADMIN);
        User owner = user(2, Role.OWNER);
        Restaurant restaurant = restaurant(owner);

        assertDoesNotThrow(() -> Access.requireOwner(admin, restaurant));
        assertDoesNotThrow(() -> Access.requireOwner(owner, restaurant));
    }

    @Test
    void anotherOwnerAndNonOwnerCannotManageRestaurant() {
        Restaurant restaurant = restaurant(user(2, Role.OWNER));

        assertForbidden(() -> Access.requireOwner(user(3, Role.OWNER), restaurant));
        assertForbidden(() -> Access.requireOwner(user(4, Role.CUSTOMER), restaurant));
    }

    @Test
    void orderParticipantsAndAdminCanReadOrder() {
        User customer = user(10, Role.CUSTOMER);
        User owner = user(20, Role.OWNER);
        User partnerUser = user(30, Role.PARTNER);
        CustomerOrder order = order(customer, owner, partnerUser);

        assertDoesNotThrow(() -> Access.requireOrderAccess(user(1, Role.ADMIN), order));
        assertDoesNotThrow(() -> Access.requireOrderAccess(customer, order));
        assertDoesNotThrow(() -> Access.requireOrderAccess(owner, order));
        assertDoesNotThrow(() -> Access.requireOrderAccess(partnerUser, order));
    }

    @Test
    void unrelatedUsersAndUnassignedPartnerCannotReadOrder() {
        User customer = user(10, Role.CUSTOMER);
        User owner = user(20, Role.OWNER);
        CustomerOrder order = order(customer, owner, null);

        assertForbidden(() -> Access.requireOrderAccess(user(11, Role.CUSTOMER), order));
        assertForbidden(() -> Access.requireOrderAccess(user(21, Role.OWNER), order));
        assertForbidden(() -> Access.requireOrderAccess(user(30, Role.PARTNER), order));
    }

    private static CustomerOrder order(User customer, User owner, User partnerUser) {
        CustomerOrder order = new CustomerOrder();
        order.setCustomer(customer);
        order.setRestaurant(restaurant(owner));
        if (partnerUser != null) {
            DeliveryPartner partner = new DeliveryPartner();
            partner.setUser(partnerUser);
            order.setPartner(partner);
        }
        return order;
    }

    private static Restaurant restaurant(User owner) {
        Restaurant restaurant = new Restaurant();
        restaurant.setOwner(owner);
        return restaurant;
    }

    private static User user(long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }

    private static void assertForbidden(Runnable action) {
        ApiException exception = assertThrows(ApiException.class, action::run);
        assertEquals(403, exception.status().value());
        assertEquals("FORBIDDEN", exception.code());
    }
}
