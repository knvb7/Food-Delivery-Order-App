package com.dmg.fooddelivery.repository;

import com.dmg.fooddelivery.model.Restaurant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    @Query(
            value = "SELECT r.* FROM restaurants r WHERE :cityId IS NULL OR r.city_id = :cityId",
            countQuery =
                    "SELECT COUNT(*) FROM restaurants WHERE :cityId IS NULL OR city_id = :cityId",
            nativeQuery = true)
    Page<Restaurant> browse(@Param("cityId") Long cityId, Pageable pageable);
}
