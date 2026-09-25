package com.dmg.fooddelivery.order;

import static com.dmg.fooddelivery.order.OrderModels.*;
import com.dmg.fooddelivery.common.ApiException;
import com.dmg.fooddelivery.common.Database;
import com.dmg.fooddelivery.common.Page;
import com.dmg.fooddelivery.security.Actor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {
    private static final RowMapper<OrderRow> ORDER = (rs, row) -> new OrderRow(rs.getLong("id"), rs.getLong("customer_id"),
            rs.getLong("restaurant_id"), rs.getObject("partner_id", Long.class), OrderStatus.valueOf(rs.getString("status")),
            rs.getString("delivery_address"), rs.getBigDecimal("total"), rs.getString("request_hash"),
            rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    private final JdbcTemplate jdbc;
    private final Database database;
    public OrderRepository(JdbcTemplate jdbc, Database database) { this.jdbc = jdbc; this.database = database; }

    public OrderRow get(long id, boolean lock) {
        return jdbc.query("SELECT * FROM orders WHERE id = ?" + (lock ? " FOR UPDATE" : ""), ORDER, id)
                .stream().findFirst().orElseThrow(() -> ApiException.notFound("Order"));
    }
    public Optional<OrderRow> byKey(long customerId, String key) {
        return jdbc.query("SELECT * FROM orders WHERE customer_id = ? AND idempotency_key = ?", ORDER, customerId, key)
                .stream().findFirst();
    }
    public long create(long customerId, PlaceOrder input, String key, String hash, BigDecimal total) {
        return database.insert("""
                INSERT INTO orders(customer_id, restaurant_id, status, delivery_address, total, idempotency_key, request_hash)
                VALUES (?, ?, 'PLACED', ?, ?, ?, ?)
                """, customerId, input.restaurantId(), input.deliveryAddress().trim(), total, key, hash);
    }
    public void addLine(long orderId, Line line) {
        jdbc.update("INSERT INTO order_items(order_id, menu_item_id, name, unit_price, quantity) VALUES (?, ?, ?, ?, ?)",
                orderId, line.menuItemId(), line.name(), line.unitPrice(), line.quantity());
    }
    public List<Line> lines(long orderId) {
        return jdbc.query("SELECT * FROM order_items WHERE order_id = ? ORDER BY menu_item_id", (rs, row) ->
                new Line(rs.getLong("menu_item_id"), rs.getString("name"), rs.getBigDecimal("unit_price"), rs.getInt("quantity")), orderId);
    }
    public void setStatus(long id, OrderStatus status) {
        jdbc.update("UPDATE orders SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", status.name(), id);
    }
    public void assign(long id, long partnerId) {
        jdbc.update("UPDATE orders SET partner_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", partnerId, id);
    }
    public OrderView view(OrderRow order) {
        var payment = jdbc.queryForObject("SELECT * FROM payments WHERE order_id = ?", (rs, row) ->
                new Payment(rs.getString("status"), rs.getBigDecimal("amount"), rs.getString("reference")), order.id());
        var history = jdbc.query("SELECT * FROM order_events WHERE order_id = ? ORDER BY id", (rs, row) ->
                new Event(rs.getLong("id"), rs.getString("type"), OrderStatus.valueOf(rs.getString("status")),
                        rs.getLong("actor_id"), rs.getTimestamp("created_at").toInstant()), order.id());
        return new OrderView(order.id(), order.customerId(), order.restaurantId(), order.partnerId(), order.status(),
                order.deliveryAddress(), order.total(), "INR", lines(order.id()), payment, history, order.createdAt(), order.updatedAt());
    }
    public Page<OrderSummary> list(Actor actor, OrderStatus status, int page, int size) {
        int offset = Page.offset(page, size);
        var args = new ArrayList<Object>();
        String where = switch (actor.role()) {
            case ADMIN -> " WHERE 1 = 1";
            case CUSTOMER -> " WHERE o.customer_id = ?";
            case OWNER -> " WHERE o.restaurant_id IN (SELECT id FROM restaurants WHERE owner_id = ?)";
            case PARTNER -> " WHERE o.partner_id IN (SELECT id FROM delivery_partners WHERE user_id = ?)";
        };
        if (actor.role() != com.dmg.fooddelivery.security.Role.ADMIN) args.add(actor.id());
        if (status != null) { where += " AND o.status = ?"; args.add(status.name()); }
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM orders o" + where, Long.class, args.toArray());
        args.add(size); args.add(offset);
        var content = jdbc.query("SELECT o.* FROM orders o" + where + " ORDER BY o.id DESC LIMIT ? OFFSET ?", (rs, row) ->
                new OrderSummary(rs.getLong("id"), rs.getLong("restaurant_id"), rs.getObject("partner_id", Long.class),
                        OrderStatus.valueOf(rs.getString("status")), rs.getBigDecimal("total"), rs.getTimestamp("created_at").toInstant()), args.toArray());
        return new Page<>(content, page, size, total);
    }
}
