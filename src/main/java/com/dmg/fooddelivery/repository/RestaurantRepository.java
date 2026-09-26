package com.dmg.fooddelivery.repository;

import com.dmg.fooddelivery.model.Restaurant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    String BROWSE_FILTER =
            """
            FROM restaurants r
            WHERE (:cityId IS NULL OR r.city_id = :cityId)
              AND (:search IS NULL OR LOCATE(LOWER(:search), LOWER(r.name)) > 0)
            """;

    @Query(value = "SELECT r.* " + BROWSE_FILTER,
            countQuery = "SELECT COUNT(*) " + BROWSE_FILTER,
            nativeQuery = true)
    Page<Restaurant> browse(
            @Param("cityId") Long cityId, @Param("search") String search, Pageable pageable);
}
