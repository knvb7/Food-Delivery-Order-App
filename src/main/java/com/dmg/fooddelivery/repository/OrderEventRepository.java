package com.dmg.fooddelivery.repository;

import com.dmg.fooddelivery.model.OrderEvent;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {

    List<OrderEvent> findByOrderIdOrderByIdAsc(long orderId);

    @Query("select e.id from OrderEvent e where e.dispatched = false order by e.id")
    List<Long> findPendingIds(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from OrderEvent e where e.id = :id")
    Optional<OrderEvent> findLockedById(long id);
}
