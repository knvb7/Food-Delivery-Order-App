package com.dmg.fooddelivery.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "delivery_partners")
@Getter @Setter
public class DeliveryPartner extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", unique = true)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "city_id")
    private City city;
    @Column(nullable = false)
    private boolean active = true;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "active_order_id", unique = true)
    private CustomerOrder activeOrder;
}
