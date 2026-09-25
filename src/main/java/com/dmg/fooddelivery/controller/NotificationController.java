package com.dmg.fooddelivery.controller;

import com.dmg.fooddelivery.dto.PageResponse;
import com.dmg.fooddelivery.service.NotificationService;
import com.dmg.fooddelivery.service.NotificationService.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/notifications") @RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notifications;
    @GetMapping public PageResponse<NotificationResponse> inbox(@RequestHeader("X-User-Id") long actorId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return notifications.inbox(actorId, page, size);
    }
}
