package com.dmg.fooddelivery.dto;

import com.dmg.fooddelivery.model.Review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public final class ReviewDtos {

    private ReviewDtos() {}

    public record ReviewInput(
            @NotNull @Min(1) @Max(5) Integer rating, @NotNull @Size(max = 2000) String comment) {}

    public record ReviewResponse(
            long id,
            long orderId,
            long restaurantId,
            long customerId,
            int rating,
            String comment,
            Instant createdAt) {
        public static ReviewResponse from(Review review) {
            return new ReviewResponse(
                    review.getId(),
                    review.getOrder().getId(),
                    review.getRestaurant().getId(),
                    review.getCustomer().getId(),
                    review.getRating(),
                    review.getComment(),
                    review.getCreatedAt());
        }
    }

    public record RestaurantReviews(
            BigDecimal averageRating, PageResponse<ReviewResponse> reviews) {}
}
