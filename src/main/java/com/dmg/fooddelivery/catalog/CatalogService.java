package com.dmg.fooddelivery.catalog;

import static com.dmg.fooddelivery.catalog.CatalogModels.*;
import com.dmg.fooddelivery.common.ApiException;
import com.dmg.fooddelivery.security.Accounts;
import com.dmg.fooddelivery.security.Actor;
import com.dmg.fooddelivery.security.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
    private final CatalogRepository catalog;
    private final Accounts accounts;
    public CatalogService(CatalogRepository catalog, Accounts accounts) { this.catalog = catalog; this.accounts = accounts; }

    @Transactional public City createCity(Actor actor, CityInput input) {
        actor.require(Role.ADMIN);
        return catalog.createCity(input);
    }
    @Transactional public City updateCity(Actor actor, long id, CityInput input) {
        actor.require(Role.ADMIN); catalog.city(id, true);
        return catalog.updateCity(id, input);
    }
    @Transactional public Restaurant createRestaurant(Actor actor, RestaurantInput input) {
        actor.require(Role.ADMIN); catalog.city(input.cityId());
        if (accounts.find(input.ownerId()).role() != Role.OWNER) throw ApiException.badRequest("ownerId must identify an OWNER");
        return catalog.createRestaurant(input);
    }
    @Transactional public Restaurant updateRestaurant(Actor actor, long id, RestaurantUpdate input) {
        actor.require(Role.ADMIN); catalog.restaurant(id, true);
        return catalog.updateRestaurant(id, input);
    }
    @Transactional public MenuItem createMenu(Actor actor, long restaurantId, MenuInput input) {
        actor.requireOwner(catalog.restaurant(restaurantId).ownerId());
        return catalog.createMenu(restaurantId, input);
    }
    @Transactional public MenuItem updateMenu(Actor actor, long restaurantId, long id, MenuUpdate input) {
        checkMenuOwner(actor, restaurantId, id);
        return catalog.updateMenu(id, input);
    }
    @Transactional public MenuItem adjustStock(Actor actor, long restaurantId, long id, int delta) {
        checkMenuOwner(actor, restaurantId, id);
        if (delta == 0) throw ApiException.badRequest("Stock delta must be nonzero");
        catalog.adjustStock(id, delta);
        return catalog.menuItem(id, false);
    }
    private void checkMenuOwner(Actor actor, long restaurantId, long id) {
        actor.requireOwner(catalog.restaurant(restaurantId).ownerId());
        if (catalog.menuItem(id, true).restaurantId() != restaurantId) throw ApiException.notFound("Menu item in restaurant");
    }
}
