package com.dmg.fooddelivery.repository;

import com.dmg.fooddelivery.model.Review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByOrderId(long orderId);

    Page<Review> findByRestaurantId(long restaurantId, Pageable pageable);

    @Query("select avg(r.rating) from Review r where r.restaurant.id = :restaurantId")
    Double averageRating(long restaurantId);
}
