package org.example.datn_sd69.modules.customerOrder.dto;

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

        /**
         * Giá gốc tại thời điểm đặt hàng.
         */
        BigDecimal originalPrice,

        /**
         * Số tiền giảm trên 1 sản phẩm tại thời điểm đặt hàng.
         */
        BigDecimal discountAmount,

        /**
         * Giá cuối cùng trên 1 sản phẩm tại thời điểm đặt hàng.
         */
        BigDecimal finalPrice,

        /**
         * Thành tiền dòng = finalPrice * quantity.
         */
        BigDecimal lineTotal,

        String note,
        String image
) {
}