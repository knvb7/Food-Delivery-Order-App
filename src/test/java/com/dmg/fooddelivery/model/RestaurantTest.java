package com.dmg.fooddelivery.model;

import com.dmg.fooddelivery.common.ApiException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestaurantTest {

    private City city;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        city = new City();
        city.setActive(true);

        restaurant = new Restaurant();
        restaurant.setCity(city);
        restaurant.setActive(true);
    }

    @Test
    void restaurantWithoutHoursIsOpenAllDay() {
        assertTrue(restaurant.isOpenAt(LocalTime.MIDNIGHT));
        assertTrue(restaurant.isOpenAt(LocalTime.NOON));
        assertDoesNotThrow(() -> restaurant.requireAcceptingOrdersAt(LocalTime.NOON));
    }

    @Test
    void daytimeHoursIncludeOpeningAndExcludeClosingBoundary() {
        restaurant.setOpensAt(LocalTime.of(9, 0));
        restaurant.setClosesAt(LocalTime.of(22, 0));

        assertFalse(restaurant.isOpenAt(LocalTime.of(8, 59, 59)));
        assertTrue(restaurant.isOpenAt(LocalTime.of(9, 0)));
        assertTrue(restaurant.isOpenAt(LocalTime.of(21, 59, 59)));
        assertFalse(restaurant.isOpenAt(LocalTime.of(22, 0)));
    }

    @Test
    void overnightHoursCoverBothSidesOfMidnight() {
        restaurant.setOpensAt(LocalTime.of(18, 0));
        restaurant.setClosesAt(LocalTime.of(2, 0));

        assertTrue(restaurant.isOpenAt(LocalTime.of(18, 0)));
        assertTrue(restaurant.isOpenAt(LocalTime.of(23, 30)));
        assertTrue(restaurant.isOpenAt(LocalTime.of(1, 59, 59)));
        assertFalse(restaurant.isOpenAt(LocalTime.of(2, 0)));
        assertFalse(restaurant.isOpenAt(LocalTime.NOON));
    }

    @Test
    void incompleteOrEqualHoursRemainClosed() {
        restaurant.setOpensAt(LocalTime.of(9, 0));
        assertFalse(restaurant.isOpenAt(LocalTime.NOON));

        restaurant.setClosesAt(LocalTime.of(9, 0));
        assertFalse(restaurant.isOpenAt(LocalTime.of(9, 0)));
    }

    @Test
    void inactiveRestaurantOrCityCannotAcceptOrders() {
        restaurant.setActive(false);
        ApiException inactiveRestaurant =
                assertThrows(
                        ApiException.class,
                        () -> restaurant.requireAcceptingOrdersAt(LocalTime.NOON));
        assertEquals("Restaurant or city is not accepting new orders", inactiveRestaurant.getMessage());

        restaurant.setActive(true);
        city.setActive(false);
        assertFalse(restaurant.isOpenAt(LocalTime.NOON));
        ApiException inactiveCity =
                assertThrows(
                        ApiException.class,
                        () -> restaurant.requireAcceptingOrdersAt(LocalTime.NOON));
        assertEquals("Restaurant or city is not accepting new orders", inactiveCity.getMessage());
    }

    @Test
    void closedRestaurantReturnsOpeningHoursConflict() {
        restaurant.setOpensAt(LocalTime.of(9, 0));
        restaurant.setClosesAt(LocalTime.of(10, 0));

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> restaurant.requireAcceptingOrdersAt(LocalTime.NOON));

        assertEquals(409, exception.status().value());
        assertEquals("Restaurant is closed outside its opening hours", exception.getMessage());
    }
}
