package org.example.datn_sd69.modules.report.service;

import org.example.datn_sd69.modules.report.dto.BestSellingProductResponse;
import org.example.datn_sd69.modules.report.dto.ReportSummaryResponse;
import org.example.datn_sd69.modules.report.dto.RevenueChartResponse;

import java.util.List;

public interface OwnerReportService {

    ReportSummaryResponse getSummary(
            String filterType,
            String fromDate,
            String toDate
    );

    List<RevenueChartResponse> getRevenueChart(
            String filterType,
            String fromDate,
            String toDate
    );

    List<BestSellingProductResponse> getBestSellingProducts(
            String filterType,
            String fromDate,
            String toDate,
            String limit
    );
}