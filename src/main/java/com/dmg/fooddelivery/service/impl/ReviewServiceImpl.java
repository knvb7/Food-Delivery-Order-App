package com.dmg.fooddelivery.service.impl;

import com.dmg.fooddelivery.common.*;
import com.dmg.fooddelivery.dto.PageResponse;
import com.dmg.fooddelivery.dto.ReviewDtos.*;
import com.dmg.fooddelivery.model.*;
import com.dmg.fooddelivery.repository.*;
import com.dmg.fooddelivery.service.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviews;
    private final OrderRepository orders;
    private final RestaurantRepository restaurants;
    private final UserService users;

    @Override @Transactional public ReviewResponse create(long actorId, long orderId, ReviewInput input) {
        var customer = users.get(actorId); Access.requireRole(customer, Role.CUSTOMER);
        var order = orders.findLockedById(orderId).orElseThrow(() -> ApiException.notFound("Order"));
        Access.requireOrderAccess(customer, order);
        if (order.getStatus() != OrderStatus.DELIVERED) throw ApiException.conflict("Only delivered orders can be reviewed");
        if (reviews.existsByOrderId(orderId)) throw ApiException.conflict("This order has already been reviewed");
        var review = new Review(); review.setOrder(order); review.setRestaurant(order.getRestaurant());
        review.setCustomer(customer); review.setRating(input.rating()); review.setComment(input.comment().trim());
        return ReviewResponse.from(reviews.save(review));
    }

    @Override @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public RestaurantReviews list(long restaurantId, int page, int size) {
        var pageable = PageResponse.request(page, size);
        if (!restaurants.existsById(restaurantId)) throw ApiException.notFound("Restaurant");
        var result = reviews.findByRestaurantId(restaurantId, pageable).map(ReviewResponse::from);
        Double average = reviews.averageRating(restaurantId);
        return new RestaurantReviews(average == null ? null : BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP), PageResponse.from(result));
    }
}
