package com.dmg.fooddelivery.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "notifications",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "recipient_id"}),
        indexes = @Index(name = "ix_notification_recipient", columnList = "recipient_id,id"))
@Getter
@Setter
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private OrderEvent event;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
