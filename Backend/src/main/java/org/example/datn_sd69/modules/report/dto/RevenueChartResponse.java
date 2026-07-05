package org.example.datn_sd69.modules.report.dto;

import java.math.BigDecimal;

public record RevenueChartResponse(
        String label,
        BigDecimal revenue,
        Long totalOrders
) {
}