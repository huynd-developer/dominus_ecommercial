package org.example.datn_sd69.modules.customerOrder.dto;

import java.math.BigDecimal;

public record CustomerOrderItemResponse(
        Integer orderItemId,
        Integer productVariantId,
        Integer productId,
        String productName,
        String brandName,
        String sku,
        Integer quantity,
        BigDecimal originalPrice,
        BigDecimal discountAmount,
        BigDecimal finalPrice,
        String note,
        String image
) {
}