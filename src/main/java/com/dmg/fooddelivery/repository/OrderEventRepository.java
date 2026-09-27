package com.dmg.fooddelivery.repository;

import com.dmg.fooddelivery.model.OrderEvent;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {

    @Query(value = "SELECT * FROM order_events WHERE order_id = :orderId ORDER BY id ASC",
            nativeQuery = true)
    List<OrderEvent> findByOrderIdOrderByIdAsc(@Param("orderId") long orderId);

    @Query(value = "SELECT id FROM order_events WHERE dispatched = FALSE ORDER BY id ASC",
            nativeQuery = true)
    List<Long> findPendingIds(Pageable pageable);

    @Query(value = "SELECT * FROM order_events WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<OrderEvent> findLockedById(@Param("id") long id);
}
