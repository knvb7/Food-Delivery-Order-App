package com.dmg.fooddelivery.repository;

import com.dmg.fooddelivery.model.DeliveryPartner;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface DeliveryPartnerRepository extends JpaRepository<DeliveryPartner, Long> {
    Optional<DeliveryPartner> findByUserId(long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from DeliveryPartner p where p.id = :id")
    Optional<DeliveryPartner> findLockedById(long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from DeliveryPartner p where p.user.id = :userId")
    Optional<DeliveryPartner> findLockedByUserId(long userId);
}
