package org.example.datn_sd69.modules.customerOrder.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CustomerOrderItemResponse(
        Integer orderItemId,

        Integer productVariantId,
        Integer productId,

        String productName,
        String brandName,
        String sku,

        String capacity,
        String bottleType,

        LocalDate manufacturingDate,
        LocalDate expirationDate,

        Integer quantity,

        BigDecimal originalPrice,

        BigDecimal discountAmount,

        BigDecimal finalPrice,

        BigDecimal lineTotal,

        String note,

        String image
) {
}