package com.dmg.fooddelivery.repository;

import com.dmg.fooddelivery.model.Notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query(
            value = "SELECT n.* FROM notifications n WHERE n.recipient_id = :recipientId",
            countQuery = "SELECT COUNT(*) FROM notifications WHERE recipient_id = :recipientId",
            nativeQuery = true)
    Page<Notification> findByRecipientId(@Param("recipientId") long recipientId, Pageable pageable);
}
