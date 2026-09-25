package com.dmg.fooddelivery.repository;

import com.dmg.fooddelivery.model.Notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @EntityGraph(attributePaths = "event")
    Page<Notification> findByRecipientId(long recipientId, Pageable pageable);
}
