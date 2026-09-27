package com.dmg.fooddelivery.service.impl;

import com.dmg.fooddelivery.common.Access;
import com.dmg.fooddelivery.common.ApiException;
import com.dmg.fooddelivery.dto.PageResponse;
import com.dmg.fooddelivery.dto.SalesReportDtos.CancelledOrder;
import com.dmg.fooddelivery.dto.SalesReportDtos.DailySales;
import com.dmg.fooddelivery.dto.SalesReportDtos.PopularDish;
import com.dmg.fooddelivery.dto.SalesReportDtos.SalesReport;
import com.dmg.fooddelivery.model.CustomerOrder;
import com.dmg.fooddelivery.model.Restaurant;
import com.dmg.fooddelivery.model.Role;
import com.dmg.fooddelivery.model.User;
import com.dmg.fooddelivery.repository.RestaurantRepository;
import com.dmg.fooddelivery.repository.SalesReportRepository;
import com.dmg.fooddelivery.repository.SalesReportRepository.DailySalesRow;
import com.dmg.fooddelivery.repository.SalesReportRepository.PopularDishRow;
import com.dmg.fooddelivery.service.SalesReportService;
import com.dmg.fooddelivery.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SalesReportServiceImpl implements SalesReportService {

    @Autowired
    private SalesReportRepository salesReportRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private Clock restaurantClock;

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public SalesReport report(
            long actorId,
            long restaurantId,
            LocalDate from,
            LocalDate to,
            int top,
            int page,
            int size) {
        User user = userService.get(actorId);
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.OWNER) {
            throw ApiException.forbidden();
        }

        Restaurant restaurant =
                restaurantRepository
                        .findById(restaurantId)
                        .orElseThrow(() -> ApiException.notFound("Restaurant"));
        Access.requireOwner(user, restaurant);
        validateRange(from, to, top);
        Pageable pageable = PageResponse.request(page, size);
        // Terminal states cannot transition again, so updatedAt is their completion timestamp.
        ZoneId zone = restaurantClock.getZone();
        Instant start = from.atStartOfDay(zone).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(zone).toInstant();

        List<DailySales> dailySales = dailySales(restaurantId, from, to, start, end, zone);
        BigDecimal totalRevenue = new BigDecimal("0.00");
        long deliveredOrders = 0;
        long cancelledOrders = 0;
        long rejectedOrders = 0;
        for (DailySales day : dailySales) {
            totalRevenue = totalRevenue.add(day.revenue());
            deliveredOrders += day.deliveredOrders();
            cancelledOrders += day.cancelledOrders();
            rejectedOrders += day.rejectedOrders();
        }

        List<PopularDish> popularDishes = new ArrayList<>();
        for (PopularDishRow dish :
                salesReportRepository.popularDishes(restaurantId, start, end, top)) {
            popularDishes.add(
                    new PopularDish(
                            dish.getMenuItemId(),
                            dish.getName(),
                            dish.getQuantity(),
                            dish.getRevenue()));
        }

        Page<CustomerOrder> cancellations =
                salesReportRepository.cancelledOrders(restaurantId, start, end, pageable);

        return new SalesReport(
                restaurantId,
                from,
                to,
                zone.getId(),
                "INR",
                totalRevenue,
                deliveredOrders,
                cancelledOrders,
                rejectedOrders,
                dailySales,
                popularDishes,
                PageResponse.from(cancellations.map(CancelledOrder::from)));
    }

    private void validateRange(LocalDate from, LocalDate to, int top) {
        if (from == null
                || to == null
                || from.isAfter(to)
                || to.equals(LocalDate.MAX)
                || ChronoUnit.DAYS.between(from, to) >= 366) {
            throw ApiException.badRequest(
                    "from and to must define an inclusive range of 1 to 366 days");
        }

        if (top < 1 || top > 50) {
            throw ApiException.badRequest("top must be between 1 and 50");
        }
    }

    private List<DailySales> dailySales(
            long restaurantId,
            LocalDate from,
            LocalDate to,
            Instant start,
            Instant end,
            ZoneId zone) {
        Map<LocalDate, DailySalesRow> rowsByDate = new HashMap<>();
        for (DailySalesRow row :
                salesReportRepository.dailySales(restaurantId, start, end, zone.getId())) {
            rowsByDate.put(row.getReportDate(), row);
        }

        List<DailySales> result = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            DailySalesRow row = rowsByDate.get(date);
            if (row == null) {
                result.add(new DailySales(date, new BigDecimal("0.00"), 0, 0, 0));
            } else {
                result.add(
                        new DailySales(
                                date,
                                row.getRevenue(),
                                row.getDeliveredOrders(),
                                row.getCancelledOrders(),
                                row.getRejectedOrders()));
            }
        }

        return result;
    }
}
