package com.dmg.fooddelivery.review;

import com.dmg.fooddelivery.catalog.CatalogRepository;
import com.dmg.fooddelivery.common.ApiException;
import com.dmg.fooddelivery.common.Database;
import com.dmg.fooddelivery.common.Page;
import com.dmg.fooddelivery.order.OrderRepository;
import com.dmg.fooddelivery.order.OrderStatus;
import com.dmg.fooddelivery.security.Actor;
import com.dmg.fooddelivery.security.Role;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {
    public record ReviewInput(@NotNull @Min(1) @Max(5) Integer rating, @NotNull @Size(max = 2000) String comment) {}
    public record Review(long id, long orderId, long restaurantId, long customerId, int rating, String comment, Instant createdAt) {}
    public record Reviews(BigDecimal averageRating, Page<Review> reviews) {}
    private static final RowMapper<Review> REVIEW = (rs, row) -> new Review(rs.getLong("id"), rs.getLong("order_id"),
            rs.getLong("restaurant_id"), rs.getLong("customer_id"), rs.getInt("rating"), rs.getString("comment"), rs.getTimestamp("created_at").toInstant());
    private final JdbcTemplate jdbc;
    private final Database database;
    private final OrderRepository orders;
    private final CatalogRepository catalog;
    public ReviewService(JdbcTemplate jdbc, Database database, OrderRepository orders, CatalogRepository catalog) {
        this.jdbc = jdbc; this.database = database; this.orders = orders; this.catalog = catalog;
    }

    @Transactional
    public Review create(Actor actor, long orderId, ReviewInput input) {
        actor.require(Role.CUSTOMER);
        var order = orders.get(orderId, true);
        if (order.customerId() != actor.id()) throw ApiException.forbidden();
        if (order.status() != OrderStatus.DELIVERED) throw ApiException.conflict("Only delivered orders can be reviewed");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM reviews WHERE order_id = ?", Long.class, orderId) > 0) {
            throw ApiException.conflict("This order has already been reviewed");
        }
        long id = database.insert("INSERT INTO reviews(order_id, restaurant_id, customer_id, rating, comment) VALUES (?, ?, ?, ?, ?)",
                orderId, order.restaurantId(), actor.id(), input.rating(), input.comment().trim());
        return jdbc.queryForObject("SELECT * FROM reviews WHERE id = ?", REVIEW, id);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Reviews list(long restaurantId, int page, int size) {
        int offset = Page.offset(page, size);
        catalog.restaurant(restaurantId);
        var content = jdbc.query("SELECT * FROM reviews WHERE restaurant_id = ? ORDER BY id DESC LIMIT ? OFFSET ?", REVIEW, restaurantId, size, offset);
        long count = jdbc.queryForObject("SELECT COUNT(*) FROM reviews WHERE restaurant_id = ?", Long.class, restaurantId);
        var average = jdbc.queryForObject("SELECT ROUND(AVG(CAST(rating AS DECIMAL(3,1))), 2) FROM reviews WHERE restaurant_id = ?", BigDecimal.class, restaurantId);
        return new Reviews(average, new Page<>(content, page, size, count));
    }
}
