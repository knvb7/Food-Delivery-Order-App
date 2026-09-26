package com.dmg.fooddelivery.controller;

import com.dmg.fooddelivery.dto.PageResponse;
import com.dmg.fooddelivery.service.NotificationService;
import com.dmg.fooddelivery.service.NotificationService.NotificationResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public PageResponse<NotificationResponse> inbox(
            @RequestHeader("X-User-Id") long actorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return notificationService.inbox(actorId, page, size);
    }
}
