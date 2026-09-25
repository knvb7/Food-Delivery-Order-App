package com.dmg.fooddelivery.catalog;

import static com.dmg.fooddelivery.catalog.CatalogModels.*;
import com.dmg.fooddelivery.common.ApiException;
import com.dmg.fooddelivery.common.Database;
import com.dmg.fooddelivery.common.Page;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogRepository {
    static final RowMapper<City> CITY = (rs, row) -> new City(rs.getLong("id"), rs.getString("name"), rs.getBoolean("active"));
    static final RowMapper<Restaurant> RESTAURANT = (rs, row) -> new Restaurant(rs.getLong("id"), rs.getLong("city_id"),
            rs.getLong("owner_id"), rs.getString("name"), rs.getString("address"), rs.getBoolean("active"));
    static final RowMapper<MenuItem> MENU = (rs, row) -> new MenuItem(rs.getLong("id"), rs.getLong("restaurant_id"),
            rs.getString("name"), rs.getString("description"), rs.getBigDecimal("price"), rs.getLong("stock"), rs.getBoolean("available"));
    private final JdbcTemplate jdbc;
    private final Database database;
    public CatalogRepository(JdbcTemplate jdbc, Database database) { this.jdbc = jdbc; this.database = database; }

    public City city(long id) { return city(id, false); }
    public City city(long id, boolean lock) {
        return jdbc.query("SELECT * FROM cities WHERE id = ?" + (lock ? " FOR UPDATE" : ""), CITY, id)
                .stream().findFirst().orElseThrow(() -> ApiException.notFound("City"));
    }
    public Page<City> cities(int page, int size) {
        int offset = Page.offset(page, size);
        return new Page<>(jdbc.query("SELECT * FROM cities ORDER BY id LIMIT ? OFFSET ?", CITY, size, offset),
                page, size, jdbc.queryForObject("SELECT COUNT(*) FROM cities", Long.class));
    }
    public City createCity(CityInput input) {
        return city(database.insert("INSERT INTO cities(name, active) VALUES (?, ?)", input.name().trim(), input.active()));
    }
    public City updateCity(long id, CityInput input) {
        jdbc.update("UPDATE cities SET name = ?, active = ? WHERE id = ?", input.name().trim(), input.active(), id);
        return city(id);
    }
    public Restaurant restaurant(long id) { return restaurant(id, false); }
    public Restaurant restaurant(long id, boolean lock) {
        return jdbc.query("SELECT * FROM restaurants WHERE id = ?" + (lock ? " FOR UPDATE" : ""), RESTAURANT, id)
                .stream().findFirst().orElseThrow(() -> ApiException.notFound("Restaurant"));
    }
    public Page<Restaurant> restaurants(Long cityId, int page, int size) {
        int offset = Page.offset(page, size);
        String where = cityId == null ? "" : " WHERE city_id = ?";
        Object[] countArgs = cityId == null ? new Object[]{} : new Object[]{cityId};
        Object[] args = cityId == null ? new Object[]{size, offset} : new Object[]{cityId, size, offset};
        return new Page<>(jdbc.query("SELECT * FROM restaurants" + where + " ORDER BY id LIMIT ? OFFSET ?", RESTAURANT, args),
                page, size, jdbc.queryForObject("SELECT COUNT(*) FROM restaurants" + where, Long.class, countArgs));
    }
    public Restaurant createRestaurant(RestaurantInput input) {
        return restaurant(database.insert("INSERT INTO restaurants(city_id, owner_id, name, address, active) VALUES (?, ?, ?, ?, ?)",
                input.cityId(), input.ownerId(), input.name().trim(), input.address().trim(), input.active()));
    }
    public Restaurant updateRestaurant(long id, RestaurantUpdate input) {
        jdbc.update("UPDATE restaurants SET name = ?, address = ?, active = ? WHERE id = ?",
                input.name().trim(), input.address().trim(), input.active(), id);
        return restaurant(id);
    }
    public MenuItem menuItem(long id, boolean lock) {
        return jdbc.query("SELECT * FROM menu_items WHERE id = ?" + (lock ? " FOR UPDATE" : ""), MENU, id)
                .stream().findFirst().orElseThrow(() -> ApiException.notFound("Menu item"));
    }
    public Page<MenuItem> menu(long restaurantId, int page, int size) {
        int offset = Page.offset(page, size);
        return new Page<>(jdbc.query("SELECT * FROM menu_items WHERE restaurant_id = ? ORDER BY id LIMIT ? OFFSET ?", MENU, restaurantId, size, offset),
                page, size, jdbc.queryForObject("SELECT COUNT(*) FROM menu_items WHERE restaurant_id = ?", Long.class, restaurantId));
    }
    public MenuItem createMenu(long restaurantId, MenuInput input) {
        return menuItem(database.insert("INSERT INTO menu_items(restaurant_id, name, description, price, stock, available) VALUES (?, ?, ?, ?, ?, ?)",
                restaurantId, input.name().trim(), input.description().trim(), input.price(), input.stock(), input.available()), false);
    }
    public MenuItem updateMenu(long id, MenuUpdate input) {
        jdbc.update("UPDATE menu_items SET name = ?, description = ?, price = ?, available = ? WHERE id = ?",
                input.name().trim(), input.description().trim(), input.price(), input.available(), id);
        return menuItem(id, false);
    }
    public void adjustStock(long id, int delta) {
        if (jdbc.update("UPDATE menu_items SET stock = stock + ? WHERE id = ? AND stock + ? >= 0",
                delta, id, delta) != 1) throw ApiException.conflict("Insufficient stock");
    }
}
