package com.dmg.fooddelivery.service.impl;

import com.dmg.fooddelivery.common.Access;
import com.dmg.fooddelivery.common.ApiException;
import com.dmg.fooddelivery.dto.PageResponse;
import com.dmg.fooddelivery.dto.ReviewDtos.RestaurantReviews;
import com.dmg.fooddelivery.dto.ReviewDtos.ReviewInput;
import com.dmg.fooddelivery.dto.ReviewDtos.ReviewResponse;
import com.dmg.fooddelivery.model.CustomerOrder;
import com.dmg.fooddelivery.model.OrderStatus;
import com.dmg.fooddelivery.model.Review;
import com.dmg.fooddelivery.model.Role;
import com.dmg.fooddelivery.model.User;
import com.dmg.fooddelivery.repository.OrderRepository;
import com.dmg.fooddelivery.repository.RestaurantRepository;
import com.dmg.fooddelivery.repository.ReviewRepository;
import com.dmg.fooddelivery.service.ReviewService;
import com.dmg.fooddelivery.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private UserService userService;

    @Override
    @Transactional
    public ReviewResponse create(long actorId, long orderId, ReviewInput input) {
        User customer = userService.get(actorId);
        Access.requireRole(customer, Role.CUSTOMER);
        CustomerOrder order = orderRepository.findLockedById(orderId).orElseThrow(() -> ApiException.notFound("Order"));
        Access.requireOrderAccess(customer, order);
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw ApiException.conflict("Only delivered orders can be reviewed");
        }

        if (reviewRepository.existsByOrderId(orderId)) {
            throw ApiException.conflict("This order has already been reviewed");
        }

        Review review = new Review();
        review.setOrder(order);
        review.setRestaurant(order.getRestaurant());
        review.setCustomer(customer);
        review.setRating(input.rating());
        review.setComment(input.comment().trim());

        return ReviewResponse.from(reviewRepository.save(review));
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantReviews list(long restaurantId, int page, int size) {
        Pageable pageable = PageResponse.request(page, size);
        if (!restaurantRepository.existsById(restaurantId)) {
            throw ApiException.notFound("Restaurant");
        }

        Page<ReviewResponse> result =
                reviewRepository
                        .findByRestaurantId(restaurantId, pageable)
                        .map(ReviewResponse::from);
        Double average = reviewRepository.averageRating(restaurantId);
        BigDecimal averageRating = null;
        if (average != null) {
            averageRating = BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP);
        }

        return new RestaurantReviews(averageRating, PageResponse.from(result));
    }
}
