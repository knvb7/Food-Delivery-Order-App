package com.dmg.fooddelivery.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "app_users")
@Getter @Setter
public class User extends BaseEntity {
    @Column(nullable = false, unique = true, length = 80)
    private String username;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Role role;
}
