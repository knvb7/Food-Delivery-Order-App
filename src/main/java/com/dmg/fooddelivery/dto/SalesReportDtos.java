package com.dmg.fooddelivery.dto;

import com.dmg.fooddelivery.model.CustomerOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class SalesReportDtos {

    private SalesReportDtos() {}

    public record DailySales(
            LocalDate date,
            BigDecimal revenue,
            long deliveredOrders,
            long cancelledOrders,
            long rejectedOrders) {}

    public record PopularDish(long menuItemId, String name, long quantity, BigDecimal revenue) {}

    public record CancelledOrder(
            long orderId, BigDecimal amount, Instant placedAt, Instant cancelledAt) {
        public static CancelledOrder from(CustomerOrder order) {
            return new CancelledOrder(
                    order.getId(), order.getTotal(), order.getCreatedAt(), order.getUpdatedAt());
        }
    }

    public record SalesReport(
            long restaurantId,
            LocalDate from,
            LocalDate to,
            String timeZone,
            String currency,
            BigDecimal totalRevenue,
            long deliveredOrderCount,
            long cancelledOrderCount,
            long rejectedOrderCount,
            List<DailySales> dailySales,
            List<PopularDish> popularDishes,
            PageResponse<CancelledOrder> cancelledOrders) {}
}
