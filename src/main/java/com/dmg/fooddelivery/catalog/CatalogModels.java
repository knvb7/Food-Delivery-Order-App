package com.dmg.fooddelivery.catalog;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public final class CatalogModels {
    private CatalogModels() {}
    public record City(long id, String name, boolean active) {}
    public record Restaurant(long id, long cityId, long ownerId, String name, String address, boolean active) {}
    public record MenuItem(long id, long restaurantId, String name, String description, BigDecimal price,
                           long stock, boolean available) {}
    public record CityInput(@NotBlank @Size(max = 100) String name, @NotNull Boolean active) {}
    public record RestaurantInput(@Positive long cityId, @Positive long ownerId,
                                  @NotBlank @Size(max = 150) String name,
                                  @NotBlank @Size(max = 500) String address, @NotNull Boolean active) {}
    public record RestaurantUpdate(@NotBlank @Size(max = 150) String name,
                                   @NotBlank @Size(max = 500) String address, @NotNull Boolean active) {}
    public record MenuInput(@NotBlank @Size(max = 150) String name,
                            @NotNull @Size(max = 1000) String description,
                            @NotNull @DecimalMin("0.01") @DecimalMax("100000.00") @Digits(integer = 6, fraction = 2) BigDecimal price,
                            @NotNull @Min(0) @Max(1000000) Integer stock, @NotNull Boolean available) {}
    public record MenuUpdate(@NotBlank @Size(max = 150) String name,
                             @NotNull @Size(max = 1000) String description,
                             @NotNull @DecimalMin("0.01") @DecimalMax("100000.00") @Digits(integer = 6, fraction = 2) BigDecimal price,
                             @NotNull Boolean available) {}
    public record StockAdjustment(@NotNull @Min(-1000000) @Max(1000000) Integer delta) {}
}
