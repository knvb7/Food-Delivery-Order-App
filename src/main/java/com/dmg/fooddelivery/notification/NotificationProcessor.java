package com.dmg.fooddelivery.notification;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** One transaction per delivery: inbox insertion and acknowledgement succeed or roll back together. */
@Service
public class NotificationProcessor {
    private final JdbcTemplate jdbc;
    public NotificationProcessor(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    public void process(long outboxId) {
        var rows = jdbc.query("SELECT processed FROM notification_outbox WHERE id = ? FOR UPDATE",
                (rs, row) -> rs.getBoolean("processed"), outboxId);
        if (rows.isEmpty() || rows.get(0)) return;
        jdbc.update("""
                INSERT INTO notifications(outbox_id, recipient_id, order_id, event_type, status)
                SELECT b.id, b.recipient_id, e.order_id, e.type, e.status
                FROM notification_outbox b JOIN order_events e ON b.event_id = e.id WHERE b.id = ?
                """, outboxId);
        jdbc.update("UPDATE notification_outbox SET processed = TRUE WHERE id = ?", outboxId);
    }
}
