package com.dmg.fooddelivery.service;

import com.dmg.fooddelivery.dto.ReviewDtos.*;

public interface ReviewService {
    ReviewResponse create(long actorId, long orderId, ReviewInput input);
    RestaurantReviews list(long restaurantId, int page, int size);
}
