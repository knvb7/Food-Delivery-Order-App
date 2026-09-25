package com.dmg.fooddelivery.repository;

import com.dmg.fooddelivery.model.CustomerOrder;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    @Query(
            value =
                    """
                    SELECT *
                    FROM orders
                    WHERE customer_id = :customerId
                      AND idempotency_key = :idempotencyKey
                    """,
            nativeQuery = true)
    Optional<CustomerOrder> findByCustomerIdAndIdempotencyKey(
            @Param("customerId") long customerId, @Param("idempotencyKey") String idempotencyKey);

    @Query(value = "SELECT * FROM orders WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<CustomerOrder> findLockedById(@Param("id") long id);

    @Query(
            value =
                    """
                    SELECT o.*
                    FROM orders o
                    JOIN restaurants r ON r.id = o.restaurant_id
                    WHERE (:status IS NULL OR o.status = :status)
                      AND (
                          :role = 'ADMIN'
                          OR (:role = 'CUSTOMER' AND o.customer_id = :userId)
                          OR (:role = 'OWNER' AND r.owner_id = :userId)
                          OR (:role = 'PARTNER' AND o.partner_id IN (
                              SELECT p.id FROM delivery_partners p WHERE p.user_id = :userId
                          ))
                      )
                    """,
            countQuery =
                    """
                    SELECT COUNT(*)
                    FROM orders o
                    JOIN restaurants r ON r.id = o.restaurant_id
                    WHERE (:status IS NULL OR o.status = :status)
                      AND (
                          :role = 'ADMIN'
                          OR (:role = 'CUSTOMER' AND o.customer_id = :userId)
                          OR (:role = 'OWNER' AND r.owner_id = :userId)
                          OR (:role = 'PARTNER' AND o.partner_id IN (
                              SELECT p.id FROM delivery_partners p WHERE p.user_id = :userId
                          ))
                      )
                    """,
            nativeQuery = true)
    Page<CustomerOrder> findVisible(
            @Param("userId") long userId,
            @Param("role") String role,
            @Param("status") String status,
            Pageable pageable);

    @Query(
            value =
                    """
                    SELECT o.*
                    FROM orders o
                    JOIN restaurants r ON r.id = o.restaurant_id
                    WHERE r.city_id = :cityId
                      AND o.partner_id IS NULL
                      AND o.status IN ('ACCEPTED', 'PREPARING')
                    """,
            countQuery =
                    """
                    SELECT COUNT(*)
                    FROM orders o
                    JOIN restaurants r ON r.id = o.restaurant_id
                    WHERE r.city_id = :cityId
                      AND o.partner_id IS NULL
                      AND o.status IN ('ACCEPTED', 'PREPARING')
                    """,
            nativeQuery = true)
    Page<CustomerOrder> findAvailable(@Param("cityId") long cityId, Pageable pageable);
}
