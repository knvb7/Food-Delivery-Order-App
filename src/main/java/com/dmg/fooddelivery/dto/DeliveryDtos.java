package com.dmg.fooddelivery.dto;

import com.dmg.fooddelivery.model.CustomerOrder;
import com.dmg.fooddelivery.model.DeliveryPartner;
import com.dmg.fooddelivery.model.OrderStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public final class DeliveryDtos {

    private DeliveryDtos() {}

    public record PartnerInput(
            @Positive long userId, @Positive long cityId, @NotNull Boolean active) {}

    public record PartnerUpdate(@Positive long cityId, @NotNull Boolean active) {}

    public record PartnerResponse(
            long id, long userId, long cityId, boolean active, Long activeOrderId) {
        public static PartnerResponse from(DeliveryPartner partner) {
            return new PartnerResponse(
                    partner.getId(),
                    partner.getUser().getId(),
                    partner.getCity().getId(),
                    partner.isActive(),
                    partner.getActiveOrder() == null ? null : partner.getActiveOrder().getId());
        }
    }

    public record AvailableOrder(
            long id, long restaurantId, String restaurantName, long cityId, OrderStatus status) {
        public static AvailableOrder from(CustomerOrder order) {
            return new AvailableOrder(
                    order.getId(),
                    order.getRestaurant().getId(),
                    order.getRestaurant().getName(),
                    order.getRestaurant().getCity().getId(),
                    order.getStatus());
        }
    }
}
