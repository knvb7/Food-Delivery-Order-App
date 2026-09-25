package com.dmg.fooddelivery.repository;

import com.dmg.fooddelivery.model.MenuItem;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    @Query(
            value = "SELECT m.* FROM menu_items m WHERE m.restaurant_id = :restaurantId",
            countQuery = "SELECT COUNT(*) FROM menu_items WHERE restaurant_id = :restaurantId",
            nativeQuery = true)
    Page<MenuItem> findByRestaurantId(@Param("restaurantId") long restaurantId, Pageable pageable);

    @Query(value = "SELECT * FROM menu_items WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<MenuItem> findLockedById(@Param("id") long id);
}
