package com.dmg.fooddelivery.delivery;

import static com.dmg.fooddelivery.delivery.DeliveryModels.*;
import com.dmg.fooddelivery.common.ApiException;
import com.dmg.fooddelivery.common.Database;
import com.dmg.fooddelivery.common.Page;
import com.dmg.fooddelivery.order.OrderStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class DeliveryRepository {
    private static final RowMapper<Partner> PARTNER = (rs, row) -> new Partner(rs.getLong("id"), rs.getLong("user_id"),
            rs.getLong("city_id"), rs.getBoolean("active"), rs.getObject("active_order_id", Long.class));
    private final JdbcTemplate jdbc;
    private final Database database;
    public DeliveryRepository(JdbcTemplate jdbc, Database database) { this.jdbc = jdbc; this.database = database; }

    public Partner byUser(long userId, boolean lock) {
        return jdbc.query("SELECT * FROM delivery_partners WHERE user_id = ?" + (lock ? " FOR UPDATE" : ""), PARTNER, userId)
                .stream().findFirst().orElseThrow(() -> ApiException.notFound("Delivery partner profile"));
    }
    public Partner get(long id, boolean lock) {
        return jdbc.query("SELECT * FROM delivery_partners WHERE id = ?" + (lock ? " FOR UPDATE" : ""), PARTNER, id)
                .stream().findFirst().orElseThrow(() -> ApiException.notFound("Delivery partner"));
    }
    public Partner create(PartnerInput input) {
        return get(database.insert("INSERT INTO delivery_partners(user_id, city_id, active) VALUES (?, ?, ?)",
                input.userId(), input.cityId(), input.active()), false);
    }
    public Partner update(long id, PartnerUpdate input) {
        jdbc.update("UPDATE delivery_partners SET city_id = ?, active = ? WHERE id = ?", input.cityId(), input.active(), id);
        return get(id, false);
    }
    public Page<Partner> list(int page, int size) {
        int offset = Page.offset(page, size);
        return new Page<>(jdbc.query("SELECT * FROM delivery_partners ORDER BY id LIMIT ? OFFSET ?", PARTNER, size, offset),
                page, size, jdbc.queryForObject("SELECT COUNT(*) FROM delivery_partners", Long.class));
    }
    public void occupy(long id, long orderId) {
        if (jdbc.update("UPDATE delivery_partners SET active_order_id = ? WHERE id = ? AND active_order_id IS NULL", orderId, id) != 1) {
            throw ApiException.conflict("Partner already has an active order");
        }
    }
    public void release(long id, long orderId) {
        if (jdbc.update("UPDATE delivery_partners SET active_order_id = NULL WHERE id = ? AND active_order_id = ?", id, orderId) != 1) {
            throw ApiException.conflict("Partner assignment does not match the order");
        }
    }
    public Page<AvailableOrder> available(long cityId, int page, int size) {
        int offset = Page.offset(page, size);
        String from = " FROM orders o JOIN restaurants r ON r.id = o.restaurant_id WHERE r.city_id = ? AND o.partner_id IS NULL AND o.status IN ('ACCEPTED', 'PREPARING')";
        var content = jdbc.query("SELECT o.id, o.restaurant_id, r.name, r.city_id, o.status" + from + " ORDER BY o.id LIMIT ? OFFSET ?", (rs, row) ->
                new AvailableOrder(rs.getLong("id"), rs.getLong("restaurant_id"), rs.getString("name"), rs.getLong("city_id"),
                        OrderStatus.valueOf(rs.getString("status"))), cityId, size, offset);
        return new Page<>(content, page, size, jdbc.queryForObject("SELECT COUNT(*)" + from, Long.class, cityId));
    }
}
