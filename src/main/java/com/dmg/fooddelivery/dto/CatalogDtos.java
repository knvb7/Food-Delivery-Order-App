package com.dmg.fooddelivery.dto;

import com.dmg.fooddelivery.model.City;
import com.dmg.fooddelivery.model.MenuItem;
import com.dmg.fooddelivery.model.Restaurant;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZonedDateTime;

public final class CatalogDtos {

    private CatalogDtos() {}

    public record CityInput(@NotBlank @Size(max = 100) String name, @NotNull Boolean active) {}

    public record RestaurantInput(
            @Positive long cityId,
            @Positive long ownerId,
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Size(max = 500) String address,
            @NotNull Boolean active) {}

    public record RestaurantUpdate(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Size(max = 500) String address,
            @NotNull Boolean active) {}

    public record OpeningHoursInput(LocalTime opensAt, LocalTime closesAt) {}

    public record MenuInput(
            @NotBlank @Size(max = 150) String name,
            @NotNull @Size(max = 1000) String description,
            @NotNull @DecimalMin("0.01") @DecimalMax("100000.00") @Digits(integer = 6, fraction = 2)
                    BigDecimal price,
            @NotNull @Min(0) @Max(1000000) Long stock,
            @NotNull Boolean available) {}

    public record MenuUpdate(
            @NotBlank @Size(max = 150) String name,
            @NotNull @Size(max = 1000) String description,
            @NotNull @DecimalMin("0.01") @DecimalMax("100000.00") @Digits(integer = 6, fraction = 2)
                    BigDecimal price,
            @NotNull Boolean available) {}

    public record StockAdjustment(@NotNull @Min(-1000000) @Max(1000000) Long delta) {}

    public record CityResponse(long id, String name, boolean active) {
        public static CityResponse from(City city) {
            return new CityResponse(city.getId(), city.getName(), city.isActive());
        }
    }

    public record RestaurantResponse(
            long id,
            long cityId,
            long ownerId,
            String name,
            String address,
            boolean active,
            LocalTime opensAt,
            LocalTime closesAt,
            String timeZone,
            boolean openNow) {
        public static RestaurantResponse from(Restaurant restaurant, ZonedDateTime now) {
            return new RestaurantResponse(
                    restaurant.getId(),
                    restaurant.getCity().getId(),
                    restaurant.getOwner().getId(),
                    restaurant.getName(),
                    restaurant.getAddress(),
                    restaurant.isActive(),
                    restaurant.getOpensAt(),
                    restaurant.getClosesAt(),
                    now.getZone().getId(),
                    restaurant.isOpenAt(now.toLocalTime()));
        }
    }

    public record MenuResponse(
            long id,
            long restaurantId,
            String name,
            String description,
            BigDecimal price,
            long stock,
            boolean available) {
        public static MenuResponse from(MenuItem menuItem) {
            return new MenuResponse(
                    menuItem.getId(),
                    menuItem.getRestaurant().getId(),
                    menuItem.getName(),
                    menuItem.getDescription(),
                    menuItem.getPrice(),
                    menuItem.getStock(),
                    menuItem.isAvailable());
        }
    }
}
