package com.dmg.fooddelivery.repository;

import com.dmg.fooddelivery.model.Notification;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @EntityGraph(attributePaths = "event")
    Page<Notification> findByRecipientId(long recipientId, Pageable pageable);
}
