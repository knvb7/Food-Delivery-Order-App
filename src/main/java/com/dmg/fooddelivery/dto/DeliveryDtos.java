package com.dmg.fooddelivery.dto;

import com.dmg.fooddelivery.model.*;
import jakarta.validation.constraints.*;

public final class DeliveryDtos {
    private DeliveryDtos() {}
    public record PartnerInput(@Positive long userId, @Positive long cityId, @NotNull Boolean active) {}
    public record PartnerUpdate(@Positive long cityId, @NotNull Boolean active) {}
    public record PartnerResponse(long id, long userId, long cityId, boolean active, Long activeOrderId) {
        public static PartnerResponse from(DeliveryPartner p) {
            return new PartnerResponse(p.getId(), p.getUser().getId(), p.getCity().getId(), p.isActive(),
                    p.getActiveOrder() == null ? null : p.getActiveOrder().getId());
        }
    }
    public record AvailableOrder(long id, long restaurantId, String restaurantName, long cityId, OrderStatus status) {
        public static AvailableOrder from(CustomerOrder o) {
            return new AvailableOrder(o.getId(), o.getRestaurant().getId(), o.getRestaurant().getName(), o.getRestaurant().getCity().getId(), o.getStatus());
        }
    }
}
