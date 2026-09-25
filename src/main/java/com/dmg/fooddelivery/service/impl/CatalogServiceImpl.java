package com.dmg.fooddelivery.service.impl;

import com.dmg.fooddelivery.common.Access;
import com.dmg.fooddelivery.common.ApiException;
import com.dmg.fooddelivery.dto.CatalogDtos.CityInput;
import com.dmg.fooddelivery.dto.CatalogDtos.CityResponse;
import com.dmg.fooddelivery.dto.CatalogDtos.MenuInput;
import com.dmg.fooddelivery.dto.CatalogDtos.MenuResponse;
import com.dmg.fooddelivery.dto.CatalogDtos.MenuUpdate;
import com.dmg.fooddelivery.dto.CatalogDtos.RestaurantInput;
import com.dmg.fooddelivery.dto.CatalogDtos.RestaurantResponse;
import com.dmg.fooddelivery.dto.CatalogDtos.RestaurantUpdate;
import com.dmg.fooddelivery.dto.PageResponse;
import com.dmg.fooddelivery.model.City;
import com.dmg.fooddelivery.model.MenuItem;
import com.dmg.fooddelivery.model.Restaurant;
import com.dmg.fooddelivery.model.Role;
import com.dmg.fooddelivery.model.User;
import com.dmg.fooddelivery.repository.CityRepository;
import com.dmg.fooddelivery.repository.MenuItemRepository;
import com.dmg.fooddelivery.repository.RestaurantRepository;
import com.dmg.fooddelivery.service.CatalogService;
import com.dmg.fooddelivery.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogServiceImpl implements CatalogService {

    private final CityRepository cities;
    private final RestaurantRepository restaurants;
    private final MenuItemRepository menuItems;
    private final UserService users;

    @Override
    public PageResponse<CityResponse> cities(int page, int size) {
        return PageResponse.from(
                cities.findAll(PageResponse.request(page, size)).map(CityResponse::from));
    }

    @Override
    @Transactional
    public CityResponse createCity(long actorId, CityInput input) {
        Access.requireRole(users.get(actorId), Role.ADMIN);

        City city = new City();
        city.setName(input.name().trim());
        city.setActive(input.active());

        return CityResponse.from(cities.save(city));
    }

    @Override
    @Transactional
    public CityResponse updateCity(long actorId, long id, CityInput input) {
        Access.requireRole(users.get(actorId), Role.ADMIN);
        City city = city(id);
        city.setName(input.name().trim());
        city.setActive(input.active());

        return CityResponse.from(city);
    }

    @Override
    public PageResponse<RestaurantResponse> restaurants(Long cityId, int page, int size) {
        return PageResponse.from(
                restaurants
                        .browse(cityId, PageResponse.request(page, size))
                        .map(RestaurantResponse::from));
    }

    @Override
    public RestaurantResponse restaurant(long id) {
        return RestaurantResponse.from(findRestaurant(id));
    }

    @Override
    @Transactional
    public RestaurantResponse createRestaurant(long actorId, RestaurantInput input) {
        Access.requireRole(users.get(actorId), Role.ADMIN);
        User owner = users.get(input.ownerId());
        if (owner.getRole() != Role.OWNER) {
            throw ApiException.badRequest("ownerId must identify an OWNER");
        }

        Restaurant restaurant = new Restaurant();
        restaurant.setCity(city(input.cityId()));
        restaurant.setOwner(owner);
        restaurant.setName(input.name().trim());
        restaurant.setAddress(input.address().trim());
        restaurant.setActive(input.active());

        return RestaurantResponse.from(restaurants.save(restaurant));
    }

    @Override
    @Transactional
    public RestaurantResponse updateRestaurant(long actorId, long id, RestaurantUpdate input) {
        Access.requireRole(users.get(actorId), Role.ADMIN);
        Restaurant restaurant = findRestaurant(id);
        restaurant.setName(input.name().trim());
        restaurant.setAddress(input.address().trim());
        restaurant.setActive(input.active());

        return RestaurantResponse.from(restaurant);
    }

    @Override
    public PageResponse<MenuResponse> menu(long restaurantId, int page, int size) {
        findRestaurant(restaurantId);

        return PageResponse.from(
                menuItems
                        .findByRestaurantId(restaurantId, PageResponse.request(page, size))
                        .map(MenuResponse::from));
    }

    @Override
    @Transactional
    public MenuResponse createMenu(long actorId, long restaurantId, MenuInput input) {
        Restaurant restaurant = findRestaurant(restaurantId);
        Access.requireOwner(users.get(actorId), restaurant);

        MenuItem item = new MenuItem();
        item.setRestaurant(restaurant);
        item.setName(input.name().trim());
        item.setDescription(input.description().trim());
        item.setPrice(input.price());
        item.setStock(input.stock());
        item.setAvailable(input.available());

        return MenuResponse.from(menuItems.save(item));
    }

    @Override
    @Transactional
    public MenuResponse updateMenu(long actorId, long restaurantId, long id, MenuUpdate input) {
        MenuItem item = ownedItem(actorId, restaurantId, id);
        item.setName(input.name().trim());
        item.setDescription(input.description().trim());
        item.setPrice(input.price());
        item.setAvailable(input.available());

        return MenuResponse.from(item);
    }

    @Override
    @Transactional
    public MenuResponse adjustStock(long actorId, long restaurantId, long id, long delta) {
        MenuItem item = ownedItem(actorId, restaurantId, id);
        if (delta == 0) {
            throw ApiException.badRequest("Stock delta must be nonzero");
        }

        item.adjustStock(delta);

        return MenuResponse.from(item);
    }

    private MenuItem ownedItem(long actorId, long restaurantId, long id) {
        Access.requireOwner(users.get(actorId), findRestaurant(restaurantId));
        MenuItem item =
                menuItems.findLockedById(id).orElseThrow(() -> ApiException.notFound("Menu item"));
        if (!item.getRestaurant().getId().equals(restaurantId)) {
            throw ApiException.notFound("Menu item in restaurant");
        }

        return item;
    }

    private City city(long id) {
        return cities.findById(id).orElseThrow(() -> ApiException.notFound("City"));
    }

    private Restaurant findRestaurant(long id) {
        return restaurants.findById(id).orElseThrow(() -> ApiException.notFound("Restaurant"));
    }
}
