package com.dmg.fooddelivery.delivery;

import com.dmg.fooddelivery.order.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public final class DeliveryModels {
    private DeliveryModels() {}
    public record Partner(long id, long userId, long cityId, boolean active, Long activeOrderId) {}
    public record PartnerInput(@Positive long userId, @Positive long cityId, @NotNull Boolean active) {}
    public record PartnerUpdate(@Positive long cityId, @NotNull Boolean active) {}
    public record AvailableOrder(long id, long restaurantId, String restaurantName, long cityId, OrderStatus status) {}
}
