package com.dmg.fooddelivery.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Check;

@Entity @Table(name = "reviews", indexes = @Index(name = "ix_review_restaurant", columnList = "restaurant_id"))
@Check(constraints = "rating BETWEEN 1 AND 5")
@Getter @Setter
public class Review extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id", unique = true)
    private CustomerOrder order;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "customer_id")
    private User customer;
    @Column(nullable = false)
    private int rating;
    @Column(nullable = false, length = 2000)
    private String comment;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
