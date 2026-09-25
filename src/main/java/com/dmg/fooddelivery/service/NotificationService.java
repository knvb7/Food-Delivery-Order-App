package com.dmg.fooddelivery.service;

import com.dmg.fooddelivery.dto.PageResponse;
import com.dmg.fooddelivery.model.CustomerOrder;
import com.dmg.fooddelivery.model.OrderStatus;

import java.time.Instant;
import java.util.List;

public interface NotificationService {

    record NotificationResponse(
            long id, long orderId, String type, OrderStatus status, Instant createdAt) {}

    void recordEvent(CustomerOrder order, String type, long actorId);

    List<Long> pendingEvents();

    void dispatch(long eventId);

    PageResponse<NotificationResponse> inbox(long actorId, int page, int size);
}
