package com.dmg.fooddelivery.service;

import com.dmg.fooddelivery.dto.ReorderDtos.ReorderBasket;

public interface ReorderService {

    ReorderBasket buildBasket(long actorId, long orderId);
}
