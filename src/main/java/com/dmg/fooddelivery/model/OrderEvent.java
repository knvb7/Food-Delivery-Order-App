package com.dmg.fooddelivery.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** An immutable status snapshot; pending events also form the durable notification queue. */
@Entity
@Table(
        name = "order_events",
        indexes = {
            @Index(name = "ix_event_order", columnList = "order_id"),
            @Index(name = "ix_event_pending", columnList = "dispatched,id")
        })
@Getter
@Setter
public class OrderEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private CustomerOrder order;

    @Column(nullable = false, length = 30)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(nullable = false)
    private Long actorId;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private Long ownerId;

    private Long partnerUserId;

    @Column(nullable = false)
    private boolean dispatched;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
