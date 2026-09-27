package com.dmg.fooddelivery.model;

import com.dmg.fooddelivery.common.ApiException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MenuItemTest {

    private MenuItem menuItem;

    @BeforeEach
    void setUp() {
        menuItem = new MenuItem();
        menuItem.setId(42L);
        menuItem.setStock(10);
    }

    @Test
    void adjustsStockUpAndDown() {
        menuItem.adjustStock(5);
        assertEquals(15, menuItem.getStock());

        menuItem.adjustStock(-7);
        assertEquals(8, menuItem.getStock());
    }

    @Test
    void permitsStockToReachZero() {
        menuItem.adjustStock(-10);

        assertEquals(0, menuItem.getStock());
    }

    @Test
    void rejectsAdjustmentBelowZeroWithoutChangingStock() {
        ApiException exception = assertThrows(ApiException.class, () -> menuItem.adjustStock(-11));

        assertEquals(409, exception.status().value());
        assertEquals("Insufficient stock for menu item 42", exception.getMessage());
        assertEquals(10, menuItem.getStock());
    }

    @Test
    void rejectsLongOverflowWithoutChangingStock() {
        menuItem.setStock(Long.MAX_VALUE);

        assertThrows(ArithmeticException.class, () -> menuItem.adjustStock(1));
        assertEquals(Long.MAX_VALUE, menuItem.getStock());
    }
}
