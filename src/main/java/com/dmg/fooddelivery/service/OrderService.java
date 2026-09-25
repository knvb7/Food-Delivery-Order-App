package com.dmg.fooddelivery.service;

import com.dmg.fooddelivery.dto.OrderDtos.*;
import com.dmg.fooddelivery.dto.PageResponse;
import com.dmg.fooddelivery.model.OrderStatus;

public interface OrderService {
    Placement place(long actorId, PlaceOrder input, String key);
    OrderResponse get(long actorId, long id);
    PageResponse<OrderSummary> list(long actorId, OrderStatus status, int page, int size);
    OrderResponse transition(long actorId, long id, OrderStatus target);
    OrderResponse claim(long actorId, long id);
}
