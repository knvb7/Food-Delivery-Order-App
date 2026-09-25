package com.dmg.fooddelivery.config;

import com.dmg.fooddelivery.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;

@Configuration @EnableScheduling @RequiredArgsConstructor @Slf4j
@ConditionalOnProperty(name = "app.notifications.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationWorker {
    private final NotificationService notifications;

    @Scheduled(fixedDelayString = "${app.notifications.delay-ms:500}")
    public void dispatch() {
        for (long id : notifications.pendingEvents()) {
            try { notifications.dispatch(id); }
            catch (RuntimeException ex) { log.warn("Notification event {} remains pending and will be retried", id, ex); }
        }
    }
}
