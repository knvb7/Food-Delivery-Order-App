package com.dmg.fooddelivery.controller;

import com.dmg.fooddelivery.dto.SalesReportDtos.SalesReport;
import com.dmg.fooddelivery.service.SalesReportService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/restaurants/{restaurantId}/sales-report")
public class SalesReportController {

    @Autowired
    private SalesReportService salesReportService;

    @GetMapping
    public SalesReport report(
            @RequestHeader("X-User-Id") long actorId,
            @PathVariable long restaurantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "5") int top,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return salesReportService.report(actorId, restaurantId, from, to, top, page, size);
    }
}
