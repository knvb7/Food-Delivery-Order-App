package com.dmg.fooddelivery.controller;

import com.dmg.fooddelivery.dto.CatalogDtos.*;
import com.dmg.fooddelivery.dto.PageResponse;
import com.dmg.fooddelivery.service.CatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class CatalogController {
    private final CatalogService catalog;
    @GetMapping("/cities") public PageResponse<CityResponse> cities(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return catalog.cities(page, size);
    }
    @PostMapping("/admin/cities") @ResponseStatus(HttpStatus.CREATED)
    public CityResponse createCity(@RequestHeader("X-User-Id") long actorId, @Valid @RequestBody CityInput input) {
        return catalog.createCity(actorId, input);
    }
    @PutMapping("/admin/cities/{id}") public CityResponse updateCity(@RequestHeader("X-User-Id") long actorId, @PathVariable long id, @Valid @RequestBody CityInput input) {
        return catalog.updateCity(actorId, id, input);
    }
    @GetMapping("/restaurants") public PageResponse<RestaurantResponse> restaurants(@RequestParam(required = false) Long cityId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return catalog.restaurants(cityId, page, size);
    }
    @GetMapping("/restaurants/{id}") public RestaurantResponse restaurant(@PathVariable long id) { return catalog.restaurant(id); }
    @PostMapping("/admin/restaurants") @ResponseStatus(HttpStatus.CREATED)
    public RestaurantResponse createRestaurant(@RequestHeader("X-User-Id") long actorId, @Valid @RequestBody RestaurantInput input) {
        return catalog.createRestaurant(actorId, input);
    }
    @PutMapping("/admin/restaurants/{id}") public RestaurantResponse updateRestaurant(@RequestHeader("X-User-Id") long actorId,
            @PathVariable long id, @Valid @RequestBody RestaurantUpdate input) {
        return catalog.updateRestaurant(actorId, id, input);
    }
    @GetMapping("/restaurants/{restaurantId}/menu") public PageResponse<MenuResponse> menu(@PathVariable long restaurantId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return catalog.menu(restaurantId, page, size);
    }
    @PostMapping("/restaurants/{restaurantId}/menu") @ResponseStatus(HttpStatus.CREATED)
    public MenuResponse createMenu(@RequestHeader("X-User-Id") long actorId, @PathVariable long restaurantId, @Valid @RequestBody MenuInput input) {
        return catalog.createMenu(actorId, restaurantId, input);
    }
    @PutMapping("/restaurants/{restaurantId}/menu/{id}")
    public MenuResponse updateMenu(@RequestHeader("X-User-Id") long actorId, @PathVariable long restaurantId,
            @PathVariable long id, @Valid @RequestBody MenuUpdate input) {
        return catalog.updateMenu(actorId, restaurantId, id, input);
    }
    @PostMapping("/restaurants/{restaurantId}/menu/{id}/stock")
    public MenuResponse stock(@RequestHeader("X-User-Id") long actorId, @PathVariable long restaurantId,
            @PathVariable long id, @Valid @RequestBody StockAdjustment input) {
        return catalog.adjustStock(actorId, restaurantId, id, input.delta());
    }
}
