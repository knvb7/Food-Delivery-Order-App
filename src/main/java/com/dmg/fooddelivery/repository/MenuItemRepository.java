package com.dmg.fooddelivery.repository;

import com.dmg.fooddelivery.model.MenuItem;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    String BROWSE_FILTER =
            """
            FROM menu_items m
            WHERE m.restaurant_id = :restaurantId
              AND (:search IS NULL
                   OR LOCATE(LOWER(:search), LOWER(m.name)) > 0
                   OR LOCATE(LOWER(:search), LOWER(m.description)) > 0)
            """;

    @Query(
            value = "SELECT m.* " + BROWSE_FILTER,
            countQuery = "SELECT COUNT(*) " + BROWSE_FILTER,
            nativeQuery = true)
    Page<MenuItem> browse(
            @Param("restaurantId") long restaurantId,
            @Param("search") String search,
            Pageable pageable);

    @Query(value = "SELECT * FROM menu_items WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<MenuItem> findLockedById(@Param("id") long id);
}
