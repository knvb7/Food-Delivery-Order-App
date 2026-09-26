package com.dmg.fooddelivery.service.impl;

import com.dmg.fooddelivery.common.Access;
import com.dmg.fooddelivery.common.ApiException;
import com.dmg.fooddelivery.dto.CatalogDtos.CityInput;
import com.dmg.fooddelivery.dto.CatalogDtos.CityResponse;
import com.dmg.fooddelivery.dto.CatalogDtos.MenuInput;
import com.dmg.fooddelivery.dto.CatalogDtos.MenuResponse;
import com.dmg.fooddelivery.dto.CatalogDtos.MenuUpdate;
import com.dmg.fooddelivery.dto.CatalogDtos.OpeningHoursInput;
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZonedDateTime;

@Service
public class CatalogServiceImpl implements CatalogService {

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private Clock restaurantClock;

    @Override
    public PageResponse<CityResponse> cities(int page, int size) {
        return PageResponse.from(
                cityRepository.findAll(PageResponse.request(page, size)).map(CityResponse::from));
    }

    @Override
    @Transactional
    public CityResponse createCity(long actorId, CityInput input) {
        Access.requireRole(userService.get(actorId), Role.ADMIN);

        City city = new City();
        city.setName(input.name().trim());
        city.setActive(input.active());

        return CityResponse.from(cityRepository.save(city));
    }

    @Override
    @Transactional
    public CityResponse updateCity(long actorId, long id, CityInput input) {
        Access.requireRole(userService.get(actorId), Role.ADMIN);
        City city = city(id);
        city.setName(input.name().trim());
        city.setActive(input.active());

        return CityResponse.from(city);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RestaurantResponse> restaurants(
            Long cityId, String query, int page, int size) {
        String search = normalizeSearch(query);
        Pageable pageable = PageResponse.request(page, size);
        ZonedDateTime now = ZonedDateTime.now(restaurantClock);
        Page<Restaurant> result = restaurantRepository.browse(cityId, search, pageable);

        return PageResponse.from(
                result.map(restaurant -> RestaurantResponse.from(restaurant, now)));
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantResponse restaurant(long id) {
        return restaurantResponse(findRestaurant(id));
    }

    @Override
    @Transactional
    public RestaurantResponse createRestaurant(long actorId, RestaurantInput input) {
        Access.requireRole(userService.get(actorId), Role.ADMIN);
        User owner = userService.get(input.ownerId());
        if (owner.getRole() != Role.OWNER) {
            throw ApiException.badRequest("ownerId must identify an OWNER");
        }

        Restaurant restaurant = new Restaurant();
        restaurant.setCity(city(input.cityId()));
        restaurant.setOwner(owner);
        restaurant.setName(input.name().trim());
        restaurant.setAddress(input.address().trim());
        restaurant.setActive(input.active());

        return restaurantResponse(restaurantRepository.save(restaurant));
    }

    @Override
    @Transactional
    public RestaurantResponse updateRestaurant(long actorId, long id, RestaurantUpdate input) {
        Access.requireRole(userService.get(actorId), Role.ADMIN);
        Restaurant restaurant = findRestaurant(id);
        restaurant.setName(input.name().trim());
        restaurant.setAddress(input.address().trim());
        restaurant.setActive(input.active());

        return restaurantResponse(restaurant);
    }

    @Override
    @Transactional
    public RestaurantResponse updateOpeningHours(long actorId, long id, OpeningHoursInput input) {
        Restaurant restaurant = findRestaurant(id);
        Access.requireOwner(userService.get(actorId), restaurant);

        LocalTime opensAt = input.opensAt();
        LocalTime closesAt = input.closesAt();
        if ((opensAt == null) != (closesAt == null)) {
            throw ApiException.badRequest("Provide both opensAt and closesAt, or set both to null");
        }

        if (opensAt != null && opensAt.equals(closesAt)) {
            throw ApiException.badRequest("Opening and closing times must be different");
        }

        restaurant.setOpensAt(opensAt);
        restaurant.setClosesAt(closesAt);

        return restaurantResponse(restaurant);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MenuResponse> menu(long restaurantId, String query, int page, int size) {
        findRestaurant(restaurantId);
        String search = normalizeSearch(query);
        Pageable pageable = PageResponse.request(page, size);
        Page<MenuItem> result = menuItemRepository.browse(restaurantId, search, pageable);

        return PageResponse.from(result.map(MenuResponse::from));
    }

    @Override
    @Transactional
    public MenuResponse createMenu(long actorId, long restaurantId, MenuInput input) {
        Restaurant restaurant = findRestaurant(restaurantId);
        Access.requireOwner(userService.get(actorId), restaurant);

        MenuItem item = new MenuItem();
        item.setRestaurant(restaurant);
        item.setName(input.name().trim());
        item.setDescription(input.description().trim());
        item.setPrice(input.price());
        item.setStock(input.stock());
        item.setAvailable(input.available());

        return MenuResponse.from(menuItemRepository.save(item));
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
        //writing it earlier because it should be checked before the db query
        if (delta == 0)
            throw ApiException.badRequest("Stock delta must be nonzero");
        MenuItem item = ownedItem(actorId, restaurantId, id);
        item.adjustStock(delta);

        return MenuResponse.from(item);
    }

    private String normalizeSearch(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }

        String search = query.trim();
        if (search.length() > 150) {
            throw ApiException.badRequest("Search query must be at most 150 characters");
        }

        return search;
    }

    private RestaurantResponse restaurantResponse(Restaurant restaurant) {
        return RestaurantResponse.from(restaurant, ZonedDateTime.now(restaurantClock));
    }

    private MenuItem ownedItem(long actorId, long restaurantId, long id) {
        Access.requireOwner(userService.get(actorId), findRestaurant(restaurantId));
        MenuItem item =
                menuItemRepository
                        .findLockedById(id)
                        .orElseThrow(() -> ApiException.notFound("Menu item"));
        if (!item.getRestaurant().getId().equals(restaurantId)) {
            throw ApiException.notFound("Menu item in restaurant");
        }

        return item;
    }

    private City city(long id) {
        return cityRepository.findById(id).orElseThrow(() -> ApiException.notFound("City"));
    }

    private Restaurant findRestaurant(long id) {
        return restaurantRepository
                .findById(id)
                .orElseThrow(() -> ApiException.notFound("Restaurant"));
    }
}
