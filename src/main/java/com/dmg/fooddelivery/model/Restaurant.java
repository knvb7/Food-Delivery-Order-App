package com.dmg.fooddelivery.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "restaurants", indexes = @Index(name = "ix_restaurant_city", columnList = "city_id"))
@Getter @Setter
public class Restaurant extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "city_id")
    private City city;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "owner_id")
    private User owner;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(nullable = false, length = 500)
    private String address;
    @Column(nullable = false)
    private boolean active = true;
}
