package com.dmg.fooddelivery.repository;

import com.dmg.fooddelivery.model.DeliveryPartner;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface DeliveryPartnerRepository extends JpaRepository<DeliveryPartner, Long> {

    Optional<DeliveryPartner> findByUserId(long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from DeliveryPartner p where p.id = :id")
    Optional<DeliveryPartner> findLockedById(long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from DeliveryPartner p where p.user.id = :userId")
    Optional<DeliveryPartner> findLockedByUserId(long userId);
}
