package com.dmg.fooddelivery.dto;

import com.dmg.fooddelivery.dto.OrderDtos.LineInput;

import java.math.BigDecimal;
import java.util.List;

public final class ReorderDtos {

    private ReorderDtos() {}

    public enum ItemAvailability {
        AVAILABLE,
        QUANTITY_REDUCED,
        UNAVAILABLE,
        OUT_OF_STOCK,
        NO_LONGER_OFFERED
    }

    public enum BlockReason {
        RESTAURANT_INACTIVE,
        CITY_INACTIVE,
        RESTAURANT_CLOSED,
        NO_AVAILABLE_ITEMS
    }

    public record ReorderLine(
            long menuItemId,
            String name,
            BigDecimal previousUnitPrice,
            BigDecimal unitPrice,
            int requestedQuantity,
            int quantity,
            boolean priceChanged,
            ItemAvailability availability) {}

    public record ReorderBasket(
            long sourceOrderId,
            long restaurantId,
            String deliveryAddress,
            String currency,
            BigDecimal total,
            List<ReorderLine> lines,
            List<LineInput> items,
            boolean canCheckout,
            BlockReason blockReason) {}
}
