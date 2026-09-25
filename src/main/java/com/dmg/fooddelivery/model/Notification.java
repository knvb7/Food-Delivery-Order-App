package com.dmg.fooddelivery.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notifications", uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "recipient_id"}),
        indexes = @Index(name = "ix_notification_recipient", columnList = "recipient_id,id"))
@Getter
@Setter
public class Notification extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "event_id")
    private OrderEvent event;
    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
