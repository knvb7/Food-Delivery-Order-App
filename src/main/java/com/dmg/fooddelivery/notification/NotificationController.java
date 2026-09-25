package com.dmg.fooddelivery.notification;

import com.dmg.fooddelivery.common.Page;
import com.dmg.fooddelivery.security.Accounts;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final JdbcTemplate jdbc;
    private final Accounts accounts;
    public NotificationController(JdbcTemplate jdbc, Accounts accounts) { this.jdbc = jdbc; this.accounts = accounts; }
    public record Notification(long id, long orderId, String eventType, String status, Instant createdAt) {}

    @GetMapping public Page<Notification> inbox(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        int offset = Page.offset(page, size);
        long recipient = accounts.current().id();
        var content = jdbc.query("SELECT * FROM notifications WHERE recipient_id = ? ORDER BY id DESC LIMIT ? OFFSET ?", (rs, row) ->
                new Notification(rs.getLong("id"), rs.getLong("order_id"), rs.getString("event_type"), rs.getString("status"),
                        rs.getTimestamp("created_at").toInstant()), recipient, size, offset);
        return new Page<>(content, page, size, jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE recipient_id = ?", Long.class, recipient));
    }
}
