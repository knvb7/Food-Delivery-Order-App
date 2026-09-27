package com.dmg.fooddelivery.service;

import com.dmg.fooddelivery.dto.SalesReportDtos.SalesReport;

import java.time.LocalDate;

public interface SalesReportService {

    SalesReport report(long actorId, long restaurantId, LocalDate from, LocalDate to, int top, int page, int size);
}
