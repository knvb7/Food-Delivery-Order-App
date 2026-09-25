package com.dmg.fooddelivery.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.notifications.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationWorker {
    private static final Logger log = LoggerFactory.getLogger(NotificationWorker.class);
    private final JdbcTemplate jdbc;
    private final NotificationProcessor processor;
    public NotificationWorker(JdbcTemplate jdbc, NotificationProcessor processor) { this.jdbc = jdbc; this.processor = processor; }

    @Scheduled(fixedDelayString = "${app.notifications.delay-ms:500}")
    public void dispatch() {
        var pending = jdbc.queryForList("SELECT id FROM notification_outbox WHERE processed = FALSE ORDER BY id LIMIT 100", Long.class);
        for (long id : pending) {
            try { processor.process(id); }
            catch (RuntimeException ex) {
                log.warn("Notification {} remains pending and will be retried", id, ex);
            }
        }
    }
}
