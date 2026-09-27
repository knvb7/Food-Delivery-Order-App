package com.dmg.fooddelivery.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "orders",
        uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "idempotency_key"}),
        indexes = {
            @Index(name = "ix_order_restaurant_status", columnList = "restaurant_id,status"),
            @Index(name = "ix_order_restaurant_updated", columnList = "restaurant_id,updated_at"),
            @Index(name = "ix_order_partner", columnList = "partner_id")
        })
@Check(constraints = "total > 0")
@Getter
@Setter
public class CustomerOrder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    private DeliveryPartner partner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status = OrderStatus.PLACED;

    @Column(nullable = false, length = 500)
    private String deliveryAddress;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Column(name = "idempotency_key", nullable = false, length = 80)
    private String idempotencyKey;

    @Column(nullable = false, length = 64)
    private String requestHash;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @OrderBy("menuItem.id ASC")
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private Payment payment;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = createdAt;
}
