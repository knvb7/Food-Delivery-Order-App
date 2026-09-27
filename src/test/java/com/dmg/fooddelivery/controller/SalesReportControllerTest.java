package com.dmg.fooddelivery.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dmg.fooddelivery.common.ApiException;
import com.dmg.fooddelivery.common.ApiExceptionHandler;
import com.dmg.fooddelivery.dto.PageResponse;
import com.dmg.fooddelivery.dto.SalesReportDtos.DailySales;
import com.dmg.fooddelivery.dto.SalesReportDtos.SalesReport;
import com.dmg.fooddelivery.service.SalesReportService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class SalesReportControllerTest {

    private static final String PATH = "/api/restaurants/1/sales-report";
    private static final LocalDate DAY = LocalDate.of(2026, 9, 27);

    @Mock
    private SalesReportService salesReportService;

    @InjectMocks
    private SalesReportController salesReportController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(salesReportController)
                        .setControllerAdvice(new ApiExceptionHandler())
                        .build();
    }

    @Test
    void defaultsAreBoundAndReportIsSerialized() throws Exception {
        DailySales day = new DailySales(DAY, new BigDecimal("360.00"), 1, 0, 0);
        SalesReport report =
                new SalesReport(
                        1,
                        DAY,
                        DAY,
                        "Asia/Kolkata",
                        "INR",
                        new BigDecimal("360.00"),
                        1,
                        0,
                        0,
                        List.of(day),
                        List.of(),
                        new PageResponse<>(List.of(), 0, 20, 0));
        when(salesReportService.report(2, 1, DAY, DAY, 5, 0, 20)).thenReturn(report);

        mockMvc.perform(
                        get(PATH)
                                .header("X-User-Id", 2)
                                .param("from", DAY.toString())
                                .param("to", DAY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurantId").value(1))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.timeZone").value("Asia/Kolkata"))
                .andExpect(jsonPath("$.totalRevenue").value(360.00))
                .andExpect(jsonPath("$.dailySales[0].deliveredOrders").value(1))
                .andExpect(jsonPath("$.cancelledOrders.totalElements").value(0));
    }

    @Test
    void explicitTopAndCancellationPaginationArePassedToService() throws Exception {
        mockMvc.perform(
                        get(PATH)
                                .header("X-User-Id", 1)
                                .param("from", DAY.toString())
                                .param("to", DAY.toString())
                                .param("top", "10")
                                .param("page", "2")
                                .param("size", "5"))
                .andExpect(status().isOk());

        verify(salesReportService).report(1, 1, DAY, DAY, 10, 2, 5);
    }

    @Test
    void missingDatesReturnBadRequestWithoutCallingService() throws Exception {
        mockMvc.perform(get(PATH).header("X-User-Id", 2).param("from", DAY.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verifyNoInteractions(salesReportService);
    }

    @Test
    void malformedDatesReturnBadRequestWithoutCallingService() throws Exception {
        mockMvc.perform(
                        get(PATH)
                                .header("X-User-Id", 2)
                                .param("from", "not-a-date")
                                .param("to", DAY.toString()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(salesReportService);
    }

    @Test
    void missingIdentityReturnsBadRequestWithoutCallingService() throws Exception {
        mockMvc.perform(get(PATH).param("from", DAY.toString()).param("to", DAY.toString()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(salesReportService);
    }

    @Test
    void unauthorizedUserGetsForbiddenResponse() throws Exception {
        when(salesReportService.report(3, 1, DAY, DAY, 5, 0, 20))
                .thenThrow(ApiException.forbidden());

        mockMvc.perform(
                        get(PATH)
                                .header("X-User-Id", 3)
                                .param("from", DAY.toString())
                                .param("to", DAY.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
