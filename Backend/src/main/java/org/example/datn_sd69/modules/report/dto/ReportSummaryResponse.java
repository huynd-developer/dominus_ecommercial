package org.example.datn_sd69.modules.report.dto;

import org.example.datn_sd69.modules.report.enums.ReportFilterType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReportSummaryResponse(
        ReportFilterType filterType,
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal totalRevenue,
        Long totalOrders,
        Long totalProductsSold
) {
}