package com.dmg.fooddelivery.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dmg.fooddelivery.common.ApiException;
import com.dmg.fooddelivery.dto.SalesReportDtos.DailySales;
import com.dmg.fooddelivery.dto.SalesReportDtos.SalesReport;
import com.dmg.fooddelivery.model.CustomerOrder;
import com.dmg.fooddelivery.model.Restaurant;
import com.dmg.fooddelivery.model.Role;
import com.dmg.fooddelivery.model.User;
import com.dmg.fooddelivery.repository.RestaurantRepository;
import com.dmg.fooddelivery.repository.SalesReportRepository;
import com.dmg.fooddelivery.repository.SalesReportRepository.DailySalesRow;
import com.dmg.fooddelivery.repository.SalesReportRepository.PopularDishRow;
import com.dmg.fooddelivery.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class SalesReportServiceImplTest {

    private static final LocalDate FROM = LocalDate.of(2026, 9, 25);
    private static final LocalDate TO = LocalDate.of(2026, 9, 27);

    @Mock
    private SalesReportRepository salesReportRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private SalesReportServiceImpl salesReportService;

    private User owner;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(2L);
        owner.setRole(Role.OWNER);
        restaurant = new Restaurant();
        restaurant.setId(1L);
        restaurant.setOwner(owner);
        setClock("Asia/Kolkata");
    }

    @Test
    void totalsUseAllDaysAndZeroFillGapsRegardlessOfCancellationPage() {
        allowOwner();
        DailySalesRow first = dailyRow(FROM, "100.10", 2, 1, 0);
        DailySalesRow last = dailyRow(TO, "200.20", 1, 2, 1);
        when(salesReportRepository.dailySales(anyLong(), any(), any(), anyString()))
                .thenReturn(List.of(last, first));

        PopularDishRow dish = mock(PopularDishRow.class);
        when(dish.getMenuItemId()).thenReturn(10L);
        when(dish.getName()).thenReturn("Rice Bowl");
        when(dish.getQuantity()).thenReturn(6L);
        when(dish.getRevenue()).thenReturn(new BigDecimal("300.30"));
        when(salesReportRepository.popularDishes(anyLong(), any(), any(), anyInt()))
                .thenReturn(List.of(dish));

        CustomerOrder cancelled = new CustomerOrder();
        cancelled.setId(18L);
        cancelled.setTotal(new BigDecimal("42.50"));
        cancelled.setCreatedAt(Instant.parse("2026-09-26T22:00:00Z"));
        cancelled.setUpdatedAt(Instant.parse("2026-09-27T00:00:00Z"));
        when(salesReportRepository.cancelledOrders(anyLong(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(cancelled), PageRequest.of(1, 1), 3));

        SalesReport report = salesReportService.report(2, 1, FROM, TO, 5, 1, 1);

        assertEquals(new BigDecimal("300.30"), report.totalRevenue());
        assertEquals(3, report.deliveredOrderCount());
        assertEquals(3, report.cancelledOrderCount());
        assertEquals(1, report.rejectedOrderCount());
        assertEquals("INR", report.currency());
        assertEquals("Asia/Kolkata", report.timeZone());
        assertEquals(
                List.of(FROM, FROM.plusDays(1), TO),
                report.dailySales().stream().map(DailySales::date).toList());
        assertEquals(new BigDecimal("0.00"), report.dailySales().get(1).revenue());
        assertEquals(0, report.dailySales().get(1).deliveredOrders());
        assertEquals(6, report.popularDishes().get(0).quantity());
        assertEquals(new BigDecimal("300.30"), report.popularDishes().get(0).revenue());
        assertEquals(1, report.cancelledOrders().page());
        assertEquals(3, report.cancelledOrders().totalElements());
        assertEquals(18, report.cancelledOrders().content().get(0).orderId());
        assertEquals(new BigDecimal("42.50"), report.cancelledOrders().content().get(0).amount());
        assertEquals(
                cancelled.getUpdatedAt(), report.cancelledOrders().content().get(0).cancelledAt());

        Instant start = Instant.parse("2026-09-24T18:30:00Z");
        Instant end = Instant.parse("2026-09-27T18:30:00Z");
        verify(salesReportRepository).dailySales(1, start, end, "Asia/Kolkata");
        verify(salesReportRepository).popularDishes(1, start, end, 5);
    }

    @Test
    void adminCanReadAnEmptyReportForAnotherOwnersRestaurant() {
        User admin = new User();
        admin.setId(1L);
        admin.setRole(Role.ADMIN);
        when(userService.get(1)).thenReturn(admin);
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        emptyReportRows();

        SalesReport report = salesReportService.report(1, 1, FROM, TO, 5, 0, 20);

        assertEquals(new BigDecimal("0.00"), report.totalRevenue());
        assertEquals(3, report.dailySales().size());
        assertEquals(0, report.cancelledOrderCount());
        assertEquals(0, report.deliveredOrderCount());
        assertTrue(report.popularDishes().isEmpty());
        assertTrue(report.cancelledOrders().content().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
        "2026-03-08, 2026-03-08T05:00:00Z, 2026-03-09T04:00:00Z",
        "2026-11-01, 2026-11-01T04:00:00Z, 2026-11-02T05:00:00Z"
    })
    void localDateBoundariesHandleShortAndLongDaylightSavingDays(
            String date, String start, String end) {
        allowOwner();
        setClock("America/New_York");
        emptyReportRows();
        LocalDate day = LocalDate.parse(date);

        SalesReport report = salesReportService.report(2, 1, day, day, 5, 0, 20);

        assertEquals(1, report.dailySales().size());
        verify(salesReportRepository)
                .dailySales(1, Instant.parse(start), Instant.parse(end), "America/New_York");
    }

    @Test
    void anotherOwnerCannotReadRestaurantFinancialData() {
        User otherOwner = new User();
        otherOwner.setId(3L);
        otherOwner.setRole(Role.OWNER);
        when(userService.get(3)).thenReturn(otherOwner);
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> salesReportService.report(3, 1, FROM, TO, 5, 0, 20));

        assertEquals(HttpStatus.FORBIDDEN, exception.status());
        verifyNoInteractions(salesReportRepository);
    }

    @ParameterizedTest
    @EnumSource(
            value = Role.class,
            names = {"CUSTOMER", "PARTNER"})
    void nonManagementRolesCannotReadReports(Role role) {
        owner.setRole(role);
        when(userService.get(2)).thenReturn(owner);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> salesReportService.report(2, 1, FROM, TO, 5, 0, 20));

        assertEquals(HttpStatus.FORBIDDEN, exception.status());
        verifyNoInteractions(restaurantRepository, salesReportRepository);
    }

    @Test
    void missingRestaurantReturnsNotFound() {
        when(userService.get(2)).thenReturn(owner);
        when(restaurantRepository.findById(1L)).thenReturn(Optional.empty());

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> salesReportService.report(2, 1, FROM, TO, 5, 0, 20));

        assertEquals(HttpStatus.NOT_FOUND, exception.status());
        verifyNoInteractions(salesReportRepository);
    }

    @ParameterizedTest
    @CsvSource({"2026-09-27, 2026-09-25", "2025-09-25, 2026-09-26", ", 2026-09-27", "2026-09-25,"})
    void invalidDateRangesAreRejectedBeforeReportQueries(String from, String to) {
        allowOwner();
        LocalDate start = from == null ? null : LocalDate.parse(from);
        LocalDate end = to == null ? null : LocalDate.parse(to);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> salesReportService.report(2, 1, start, end, 5, 0, 20));

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        verifyNoInteractions(salesReportRepository);
    }

    @Test
    void fullLeapYearIsAnAllowed366DayRange() {
        allowOwner();
        emptyReportRows();

        SalesReport report =
                salesReportService.report(
                        2, 1, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), 50, 0, 100);

        assertEquals(366, report.dailySales().size());
    }

    @ParameterizedTest
    @CsvSource({"0, 0, 20", "51, 0, 20", "5, -1, 20", "5, 100001, 20", "5, 0, 0", "5, 0, 101"})
    void invalidLimitsAreRejectedBeforeReportQueries(int top, int page, int size) {
        allowOwner();

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> salesReportService.report(2, 1, FROM, TO, top, page, size));

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        verifyNoInteractions(salesReportRepository);
    }

    private void allowOwner() {
        when(userService.get(2)).thenReturn(owner);
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
    }

    private void emptyReportRows() {
        when(salesReportRepository.dailySales(anyLong(), any(), any(), anyString()))
                .thenReturn(List.of());
        when(salesReportRepository.popularDishes(anyLong(), any(), any(), anyInt()))
                .thenReturn(List.of());
        when(salesReportRepository.cancelledOrders(anyLong(), any(), any(), any()))
                .thenAnswer(
                        invocation -> {
                            Pageable pageable = invocation.getArgument(3);
                            return new PageImpl<CustomerOrder>(List.of(), pageable, 0);
                        });
    }

    private DailySalesRow dailyRow(
            LocalDate date, String revenue, long delivered, long cancelled, long rejected) {
        DailySalesRow row = mock(DailySalesRow.class);
        when(row.getReportDate()).thenReturn(date);
        when(row.getRevenue()).thenReturn(new BigDecimal(revenue));
        when(row.getDeliveredOrders()).thenReturn(delivered);
        when(row.getCancelledOrders()).thenReturn(cancelled);
        when(row.getRejectedOrders()).thenReturn(rejected);
        return row;
    }

    private void setClock(String zone) {
        Clock clock = Clock.fixed(Instant.parse("2026-09-27T12:00:00Z"), ZoneId.of(zone));
        ReflectionTestUtils.setField(salesReportService, "restaurantClock", clock);
    }
}
