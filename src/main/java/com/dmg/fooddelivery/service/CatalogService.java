package com.dmg.fooddelivery.service;

import com.dmg.fooddelivery.dto.CatalogDtos.CityInput;
import com.dmg.fooddelivery.dto.CatalogDtos.CityResponse;
import com.dmg.fooddelivery.dto.CatalogDtos.MenuInput;
import com.dmg.fooddelivery.dto.CatalogDtos.MenuResponse;
import com.dmg.fooddelivery.dto.CatalogDtos.MenuUpdate;
import com.dmg.fooddelivery.dto.CatalogDtos.RestaurantInput;
import com.dmg.fooddelivery.dto.CatalogDtos.RestaurantResponse;
import com.dmg.fooddelivery.dto.CatalogDtos.RestaurantUpdate;
import com.dmg.fooddelivery.dto.PageResponse;

public interface CatalogService {

    PageResponse<CityResponse> cities(int page, int size);

    CityResponse createCity(long actorId, CityInput input);

    CityResponse updateCity(long actorId, long id, CityInput input);

    PageResponse<RestaurantResponse> restaurants(Long cityId, int page, int size);

    RestaurantResponse restaurant(long id);

    RestaurantResponse createRestaurant(long actorId, RestaurantInput input);

    RestaurantResponse updateRestaurant(long actorId, long id, RestaurantUpdate input);

    PageResponse<MenuResponse> menu(long restaurantId, int page, int size);

    MenuResponse createMenu(long actorId, long restaurantId, MenuInput input);

    MenuResponse updateMenu(long actorId, long restaurantId, long id, MenuUpdate input);

    MenuResponse adjustStock(long actorId, long restaurantId, long id, long delta);
}
