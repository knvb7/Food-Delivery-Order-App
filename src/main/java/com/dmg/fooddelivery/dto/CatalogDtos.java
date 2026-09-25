package com.dmg.fooddelivery.dto;

import com.dmg.fooddelivery.model.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public final class CatalogDtos {
    private CatalogDtos() {}
    public record CityInput(@NotBlank @Size(max = 100) String name, @NotNull Boolean active) {}
    public record RestaurantInput(@Positive long cityId, @Positive long ownerId,
                                  @NotBlank @Size(max = 150) String name,
                                  @NotBlank @Size(max = 500) String address, @NotNull Boolean active) {}
    public record RestaurantUpdate(@NotBlank @Size(max = 150) String name,
                                   @NotBlank @Size(max = 500) String address, @NotNull Boolean active) {}
    public record MenuInput(@NotBlank @Size(max = 150) String name, @NotNull @Size(max = 1000) String description,
                            @NotNull @DecimalMin("0.01") @DecimalMax("100000.00") @Digits(integer = 6, fraction = 2) BigDecimal price,
                            @NotNull @Min(0) @Max(1000000) Long stock, @NotNull Boolean available) {}
    public record MenuUpdate(@NotBlank @Size(max = 150) String name, @NotNull @Size(max = 1000) String description,
                             @NotNull @DecimalMin("0.01") @DecimalMax("100000.00") @Digits(integer = 6, fraction = 2) BigDecimal price,
                             @NotNull Boolean available) {}
    public record StockAdjustment(@NotNull @Min(-1000000) @Max(1000000) Long delta) {}
    public record CityResponse(long id, String name, boolean active) {
        public static CityResponse from(City c) { return new CityResponse(c.getId(), c.getName(), c.isActive()); }
    }
    public record RestaurantResponse(long id, long cityId, long ownerId, String name, String address, boolean active) {
        public static RestaurantResponse from(Restaurant r) {
            return new RestaurantResponse(r.getId(), r.getCity().getId(), r.getOwner().getId(), r.getName(), r.getAddress(), r.isActive());
        }
    }
    public record MenuResponse(long id, long restaurantId, String name, String description, BigDecimal price, long stock, boolean available) {
        public static MenuResponse from(MenuItem m) {
            return new MenuResponse(m.getId(), m.getRestaurant().getId(), m.getName(), m.getDescription(), m.getPrice(), m.getStock(), m.isAvailable());
        }
    }
}
