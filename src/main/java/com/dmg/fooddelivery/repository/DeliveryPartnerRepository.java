package com.dmg.fooddelivery.repository;

import com.dmg.fooddelivery.model.DeliveryPartner;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DeliveryPartnerRepository extends JpaRepository<DeliveryPartner, Long> {

    @Query(value = "SELECT * FROM delivery_partners WHERE user_id = :userId", nativeQuery = true)
    Optional<DeliveryPartner> findByUserId(@Param("userId") long userId);

    @Query(value = "SELECT * FROM delivery_partners WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<DeliveryPartner> findLockedById(@Param("id") long id);

    @Query(
            value = "SELECT * FROM delivery_partners WHERE user_id = :userId FOR UPDATE",
            nativeQuery = true)
    Optional<DeliveryPartner> findLockedByUserId(@Param("userId") long userId);
}
