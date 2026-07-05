package org.example.datn_sd69.modules.report.controller;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.report.dto.BestSellingProductResponse;
import org.example.datn_sd69.modules.report.dto.ReportSummaryResponse;
import org.example.datn_sd69.modules.report.dto.RevenueChartResponse;
import org.example.datn_sd69.modules.report.service.OwnerReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/owner/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('OWNER')")
public class OwnerReportController {

    private final OwnerReportService ownerReportService;

    @GetMapping("/summary")
    public ResponseEntity<ReportSummaryResponse> getSummary(
            @RequestParam(required = false) String filterType,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate
    ) {
        return ResponseEntity.ok(
                ownerReportService.getSummary(filterType, fromDate, toDate)
        );
    }

    @GetMapping("/revenue-chart")
    public ResponseEntity<List<RevenueChartResponse>> getRevenueChart(
            @RequestParam(required = false) String filterType,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate
    ) {
        return ResponseEntity.ok(
                ownerReportService.getRevenueChart(filterType, fromDate, toDate)
        );
    }

    @GetMapping("/best-selling-products")
    public ResponseEntity<List<BestSellingProductResponse>> getBestSellingProducts(
            @RequestParam(required = false) String filterType,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String limit
    ) {
        return ResponseEntity.ok(
                ownerReportService.getBestSellingProducts(filterType, fromDate, toDate, limit)
        );
    }
}