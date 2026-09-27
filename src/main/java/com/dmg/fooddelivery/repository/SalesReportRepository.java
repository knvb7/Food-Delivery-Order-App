package com.dmg.fooddelivery.repository;

import com.dmg.fooddelivery.model.CustomerOrder;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface SalesReportRepository extends Repository<CustomerOrder, Long> {

    String CANCELLED_FILTER =
            """
            FROM orders o
            WHERE o.restaurant_id = :restaurantId
              AND o.status = 'CANCELLED'
              AND o.updated_at >= :start
              AND o.updated_at < :end
            """;

    interface DailySalesRow {
        LocalDate getReportDate();

        BigDecimal getRevenue();

        long getDeliveredOrders();

        long getCancelledOrders();

        long getRejectedOrders();
    }

    interface PopularDishRow {
        long getMenuItemId();

        String getName();

        long getQuantity();

        BigDecimal getRevenue();
    }

    @Query(
            value =
                    """
SELECT CAST(o.updated_at AT TIME ZONE :timeZone AS DATE) AS reportDate,
       SUM(CASE WHEN o.status = 'DELIVERED' AND p.status = 'CAPTURED'
                THEN p.amount ELSE 0 END) AS revenue,
       SUM(CASE WHEN o.status = 'DELIVERED' AND p.status = 'CAPTURED'
                THEN 1 ELSE 0 END) AS deliveredOrders,
       SUM(CASE WHEN o.status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelledOrders,
       SUM(CASE WHEN o.status = 'REJECTED' THEN 1 ELSE 0 END) AS rejectedOrders
FROM orders o
JOIN payments p ON p.order_id = o.id
WHERE o.restaurant_id = :restaurantId
  AND o.updated_at >= :start
  AND o.updated_at < :end
  AND o.status IN ('DELIVERED', 'CANCELLED', 'REJECTED')
GROUP BY CAST(o.updated_at AT TIME ZONE :timeZone AS DATE)
ORDER BY reportDate
""",
            nativeQuery = true)
    List<DailySalesRow> dailySales(
            @Param("restaurantId") long restaurantId,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("timeZone") String timeZone);

    @Query(
            value =
                    """
                    SELECT m.id AS menuItemId, m.name AS name,
                           SUM(i.quantity) AS quantity,
                           SUM(i.unit_price * i.quantity) AS revenue
                    FROM order_items i
                    JOIN orders o ON o.id = i.order_id
                    JOIN payments p ON p.order_id = o.id
                    JOIN menu_items m ON m.id = i.menu_item_id
                    WHERE o.restaurant_id = :restaurantId
                      AND o.status = 'DELIVERED'
                      AND p.status = 'CAPTURED'
                      AND o.updated_at >= :start
                      AND o.updated_at < :end
                    GROUP BY m.id, m.name
                    ORDER BY quantity DESC, revenue DESC, menuItemId ASC
                    LIMIT :limit
                    """,
            nativeQuery = true)
    List<PopularDishRow> popularDishes(
            @Param("restaurantId") long restaurantId,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("limit") int limit);

    @Query(
            value = "SELECT o.* " + CANCELLED_FILTER,
            countQuery = "SELECT COUNT(*) " + CANCELLED_FILTER,
            nativeQuery = true)
    Page<CustomerOrder> cancelledOrders(
            @Param("restaurantId") long restaurantId,
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable);
}
