package org.example.datn_sd69.modules.customerOrder.dto;

import java.math.BigDecimal;

public record CustomerReturnItemResponse(
        Integer orderItemId,
        Integer productId,
        Integer productVariantId,

        String productName,
        String brandName,
        String sku,
        String image,

        String capacity,
        String bottleType,

        Integer orderedQuantity,
        Integer returnQuantity,

        BigDecimal unitFinalPrice,
        BigDecimal itemAmount,
        BigDecimal voucherAllocatedAmount,
        BigDecimal refundAmount
) {
}