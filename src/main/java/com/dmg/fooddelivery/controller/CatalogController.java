package com.dmg.fooddelivery.controller;

import com.dmg.fooddelivery.dto.CatalogDtos.CityInput;
import com.dmg.fooddelivery.dto.CatalogDtos.CityResponse;
import com.dmg.fooddelivery.dto.CatalogDtos.MenuInput;
import com.dmg.fooddelivery.dto.CatalogDtos.MenuResponse;
import com.dmg.fooddelivery.dto.CatalogDtos.MenuUpdate;
import com.dmg.fooddelivery.dto.CatalogDtos.OpeningHoursInput;
import com.dmg.fooddelivery.dto.CatalogDtos.RestaurantInput;
import com.dmg.fooddelivery.dto.CatalogDtos.RestaurantResponse;
import com.dmg.fooddelivery.dto.CatalogDtos.RestaurantUpdate;
import com.dmg.fooddelivery.dto.CatalogDtos.StockAdjustment;
import com.dmg.fooddelivery.dto.PageResponse;
import com.dmg.fooddelivery.service.CatalogService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CatalogController {

    @Autowired
    private CatalogService catalogService;

    @GetMapping("/cities")
    public PageResponse<CityResponse> cities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return catalogService.cities(page, size);
    }

    @PostMapping("/admin/cities")
    @ResponseStatus(HttpStatus.CREATED)
    public CityResponse createCity(
            @RequestHeader("X-User-Id") long actorId, @Valid @RequestBody CityInput input) {
        return catalogService.createCity(actorId, input);
    }

    @PutMapping("/admin/cities/{id}")
    public CityResponse updateCity(
            @RequestHeader("X-User-Id") long actorId,
            @PathVariable long id,
            @Valid @RequestBody CityInput input) {
        return catalogService.updateCity(actorId, id, input);
    }

    @GetMapping("/restaurants")
    public PageResponse<RestaurantResponse> restaurants(
            @RequestParam(required = false) Long cityId,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return catalogService.restaurants(cityId, query, page, size);
    }

    @GetMapping("/restaurants/{id}")
    public RestaurantResponse restaurant(@PathVariable long id) {
        return catalogService.restaurant(id);
    }

    @PostMapping("/admin/restaurants")
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantResponse createRestaurant(
            @RequestHeader("X-User-Id") long actorId, @Valid @RequestBody RestaurantInput input) {
        return catalogService.createRestaurant(actorId, input);
    }

    @PutMapping("/admin/restaurants/{id}")
    public RestaurantResponse updateRestaurant(
            @RequestHeader("X-User-Id") long actorId,
            @PathVariable long id,
            @Valid @RequestBody RestaurantUpdate input) {
        return catalogService.updateRestaurant(actorId, id, input);
    }

    @PutMapping("/restaurants/{id}/opening-hours")
    public RestaurantResponse updateOpeningHours(
            @RequestHeader("X-User-Id") long actorId,
            @PathVariable long id,
            @Valid @RequestBody OpeningHoursInput input) {
        return catalogService.updateOpeningHours(actorId, id, input);
    }

    @GetMapping("/restaurants/{restaurantId}/menu")
    public PageResponse<MenuResponse> menu(
            @PathVariable long restaurantId,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return catalogService.menu(restaurantId, query, page, size);
    }

    @PostMapping("/restaurants/{restaurantId}/menu")
    @ResponseStatus(HttpStatus.CREATED)
    public MenuResponse createMenu(
            @RequestHeader("X-User-Id") long actorId,
            @PathVariable long restaurantId,
            @Valid @RequestBody MenuInput input) {
        return catalogService.createMenu(actorId, restaurantId, input);
    }

    @PutMapping("/restaurants/{restaurantId}/menu/{id}")
    public MenuResponse updateMenu(
            @RequestHeader("X-User-Id") long actorId,
            @PathVariable long restaurantId,
            @PathVariable long id,
            @Valid @RequestBody MenuUpdate input) {
        return catalogService.updateMenu(actorId, restaurantId, id, input);
    }

    @PostMapping("/restaurants/{restaurantId}/menu/{id}/stock")
    public MenuResponse stock(
            @RequestHeader("X-User-Id") long actorId,
            @PathVariable long restaurantId,
            @PathVariable long id,
            @Valid @RequestBody StockAdjustment input) {
        return catalogService.adjustStock(actorId, restaurantId, id, input.delta());
    }
}
