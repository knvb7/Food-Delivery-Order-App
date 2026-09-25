package com.dmg.fooddelivery.dto;

import com.dmg.fooddelivery.model.Review;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

public final class ReviewDtos {
    private ReviewDtos() {}
    public record ReviewInput(@NotNull @Min(1) @Max(5) Integer rating, @NotNull @Size(max = 2000) String comment) {}
    public record ReviewResponse(long id, long orderId, long restaurantId, long customerId, int rating, String comment, Instant createdAt) {
        public static ReviewResponse from(Review r) {
            return new ReviewResponse(r.getId(), r.getOrder().getId(), r.getRestaurant().getId(), r.getCustomer().getId(),
                    r.getRating(), r.getComment(), r.getCreatedAt());
        }
    }
    public record RestaurantReviews(BigDecimal averageRating, PageResponse<ReviewResponse> reviews) {}
}
