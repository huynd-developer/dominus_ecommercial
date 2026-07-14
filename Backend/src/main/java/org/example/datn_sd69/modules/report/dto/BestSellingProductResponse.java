package org.example.datn_sd69.modules.report.dto;

import java.math.BigDecimal;

public record BestSellingProductResponse(
        Integer productId,
        String productName,
        String brandName,
        Long totalSold,
        BigDecimal revenue,
        String imageUrl
) {
}