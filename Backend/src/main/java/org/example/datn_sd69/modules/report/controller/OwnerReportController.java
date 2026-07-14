package org.example.datn_sd69.modules.report.controller;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.report.dto.BestSellingProductResponse;
import org.example.datn_sd69.modules.report.dto.ReportSummaryResponse;
import org.example.datn_sd69.modules.report.dto.RevenueChartResponse;
import org.example.datn_sd69.modules.report.service.OwnerReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/owner/reports")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAuthority('OWNER')")
public class OwnerReportController {

    private final OwnerReportService ownerReportService;

    /**
     * Tổng quan báo cáo.
     *
     * filterType:
     * - DAY
     * - WEEK
     * - MONTH
     * - YEAR
     * - CUSTOM
     *
     * Nếu không truyền filterType thì mặc định MONTH.
     *
     * CUSTOM bắt buộc truyền:
     * - fromDate=yyyy-MM-dd
     * - toDate=yyyy-MM-dd
     */
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

    /**
     * Dữ liệu biểu đồ doanh thu.
     *
     * DAY    -> group theo giờ
     * WEEK   -> group theo ngày
     * MONTH  -> group theo ngày
     * YEAR   -> group theo tháng
     * CUSTOM -> <= 31 ngày group theo ngày, còn lại group theo tháng
     */
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

    /**
     * Top sản phẩm bán chạy.
     *
     * limit mặc định 10, tối đa 50.
     */
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