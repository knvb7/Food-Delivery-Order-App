package com.dmg.fooddelivery.repository;

import com.dmg.fooddelivery.model.MenuItem;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    Page<MenuItem> findByRestaurantId(long restaurantId, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MenuItem m where m.id = :id")
    Optional<MenuItem> findLockedById(long id);
}
