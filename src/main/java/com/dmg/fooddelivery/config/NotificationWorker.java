package com.dmg.fooddelivery.config;

import com.dmg.fooddelivery.service.NotificationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@ConditionalOnProperty(
        name = "app.notifications.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class NotificationWorker {

    @Autowired
    private NotificationService notificationService;

    @Scheduled(initialDelayString = "${app.notifications.initial-delay-ms:5000}", fixedDelayString = "${app.notifications.delay-ms:5000}")
    public void dispatch() {
        for (long id : notificationService.pendingEvents()) {
            notificationService.dispatch(id);
        }
    }
}
