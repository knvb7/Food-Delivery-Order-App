package com.dmg.fooddelivery.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** An immutable status snapshot; pending events also form the durable notification queue. */
@Entity @Table(name = "order_events", indexes = {
        @Index(name = "ix_event_order", columnList = "order_id"),
        @Index(name = "ix_event_pending", columnList = "dispatched,id")})
@Getter @Setter
public class OrderEvent extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id")
    private CustomerOrder order;
    @Column(nullable = false, length = 30)
    private String type;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
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
