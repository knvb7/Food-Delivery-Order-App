package com.dmg.fooddelivery.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "cities")
@Getter @Setter
public class City extends BaseEntity {
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    @Column(nullable = false)
    private boolean active = true;
}
