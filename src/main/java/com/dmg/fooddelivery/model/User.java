package com.dmg.fooddelivery.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "app_users")
@Getter
@Setter
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(name = "full_name", length = 120)
    private String fullName;

    @Column(unique = true, length = 254)
    private String email;

    @Column(name = "phone_number", unique = true, length = 16)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;
}
