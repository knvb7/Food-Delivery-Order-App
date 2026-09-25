package com.dmg.fooddelivery.repository;

import com.dmg.fooddelivery.model.CustomerOrder;
import com.dmg.fooddelivery.model.OrderStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    Optional<CustomerOrder> findByCustomerIdAndIdempotencyKey(
            long customerId, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.id = :id")
    Optional<CustomerOrder> findLockedById(long id);

    @Query(
            """
select o from CustomerOrder o where (:status is null or o.status = :status)
and (:role = 'ADMIN' or (:role = 'CUSTOMER' and o.customer.id = :userId)
or (:role = 'OWNER' and o.restaurant.owner.id = :userId)
or (:role = 'PARTNER' and o.partner.id in (select p.id from DeliveryPartner p where p.user.id = :userId)))
""")
    Page<CustomerOrder> findVisible(
            long userId, String role, OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "restaurant")
    @Query(
            """
            select o from CustomerOrder o where o.restaurant.city.id = :cityId
            and o.partner is null and o.status in (com.dmg.fooddelivery.model.OrderStatus.ACCEPTED,
            com.dmg.fooddelivery.model.OrderStatus.PREPARING)
            """)
    Page<CustomerOrder> findAvailable(long cityId, Pageable pageable);
}
