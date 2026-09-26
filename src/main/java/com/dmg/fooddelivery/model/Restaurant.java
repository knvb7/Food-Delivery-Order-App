package com.dmg.fooddelivery.model;

import com.dmg.fooddelivery.common.ApiException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "restaurants", indexes = @Index(name = "ix_restaurant_city", columnList = "city_id"))
@Getter
@Setter
public class Restaurant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id")
    private City city;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "opens_at")
    private LocalTime opensAt;

    @Column(name = "closes_at")
    private LocalTime closesAt;

    public boolean isOpenAt(LocalTime time) {
        return active && city.isActive() && isWithinOpeningHours(time);
    }

    public void requireAcceptingOrdersAt(LocalTime time) {
        if (!active || !city.isActive()) {
            throw ApiException.conflict("Restaurant or city is not accepting new orders");
        }

        if (!isWithinOpeningHours(time)) {
            throw ApiException.conflict("Restaurant is closed outside its opening hours");
        }
    }

    private boolean isWithinOpeningHours(LocalTime time) {
        if (opensAt == null && closesAt == null) {
            return true;
        }

        if (opensAt == null || closesAt == null || opensAt.equals(closesAt)) {
            return false;
        }

        if (opensAt.isBefore(closesAt)) {
            return !time.isBefore(opensAt) && time.isBefore(closesAt);
        }

        // Overnight hours, such as 18:00 to 02:00, cross midnight.
        return !time.isBefore(opensAt) || time.isBefore(closesAt);
    }
}
