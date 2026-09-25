package com.dmg.fooddelivery.catalog;

import static com.dmg.fooddelivery.catalog.CatalogModels.*;
import com.dmg.fooddelivery.common.Page;
import com.dmg.fooddelivery.security.Accounts;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final CatalogRepository catalog;
    private final CatalogService service;
    private final Accounts accounts;
    public CatalogController(CatalogRepository catalog, CatalogService service, Accounts accounts) {
        this.catalog = catalog; this.service = service; this.accounts = accounts;
    }
    @GetMapping("/cities") public Page<City> cities(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return catalog.cities(page, size);
    }
    @PostMapping("/admin/cities") @ResponseStatus(HttpStatus.CREATED)
    public City createCity(@Valid @RequestBody CityInput input) { return service.createCity(accounts.current(), input); }
    @PutMapping("/admin/cities/{id}") public City updateCity(@PathVariable long id, @Valid @RequestBody CityInput input) {
        return service.updateCity(accounts.current(), id, input);
    }
    @GetMapping("/restaurants") public Page<Restaurant> restaurants(@RequestParam(required = false) Long cityId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return catalog.restaurants(cityId, page, size);
    }
    @GetMapping("/restaurants/{id}") public Restaurant restaurant(@PathVariable long id) { return catalog.restaurant(id); }
    @PostMapping("/admin/restaurants") @ResponseStatus(HttpStatus.CREATED)
    public Restaurant createRestaurant(@Valid @RequestBody RestaurantInput input) { return service.createRestaurant(accounts.current(), input); }
    @PutMapping("/admin/restaurants/{id}") public Restaurant updateRestaurant(@PathVariable long id, @Valid @RequestBody RestaurantUpdate input) {
        return service.updateRestaurant(accounts.current(), id, input);
    }
    @GetMapping("/restaurants/{restaurantId}/menu") public Page<MenuItem> menu(@PathVariable long restaurantId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        catalog.restaurant(restaurantId);
        return catalog.menu(restaurantId, page, size);
    }
    @PostMapping("/restaurants/{restaurantId}/menu") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public MenuItem createMenu(@PathVariable long restaurantId, @Valid @RequestBody MenuInput input) {
        return service.createMenu(accounts.current(), restaurantId, input);
    }
    @PutMapping("/restaurants/{restaurantId}/menu/{id}") @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public MenuItem updateMenu(@PathVariable long restaurantId, @PathVariable long id, @Valid @RequestBody MenuUpdate input) {
        return service.updateMenu(accounts.current(), restaurantId, id, input);
    }
    @PostMapping("/restaurants/{restaurantId}/menu/{id}/stock") @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public MenuItem stock(@PathVariable long restaurantId, @PathVariable long id, @Valid @RequestBody StockAdjustment input) {
        return service.adjustStock(accounts.current(), restaurantId, id, input.delta());
    }
}
