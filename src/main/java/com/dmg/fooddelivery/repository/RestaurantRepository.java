package com.dmg.fooddelivery.repository;

import com.dmg.fooddelivery.model.Restaurant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    @EntityGraph(attributePaths = {"city", "owner"})
    @Query("select r from Restaurant r where :cityId is null or r.city.id = :cityId")
    Page<Restaurant> browse(Long cityId, Pageable pageable);
}
