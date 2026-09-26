package com.dmg.fooddelivery.repository;

import com.dmg.fooddelivery.model.Review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query(value = "SELECT COUNT(*) > 0 FROM reviews WHERE order_id = :orderId", nativeQuery = true)
    boolean existsByOrderId(@Param("orderId") long orderId);

    @Query(
            value = "SELECT r.* FROM reviews r WHERE r.restaurant_id = :restaurantId",
            countQuery = "SELECT COUNT(*) FROM reviews WHERE restaurant_id = :restaurantId",
            nativeQuery = true)
    Page<Review> findByRestaurantId(@Param("restaurantId") long restaurantId, Pageable pageable);

    @Query(
            value =
                    """
                    SELECT AVG(CAST(rating AS DOUBLE PRECISION))
                    FROM reviews
                    WHERE restaurant_id = :restaurantId
                    """,
            nativeQuery = true)
    Double averageRating(@Param("restaurantId") long restaurantId);
}
