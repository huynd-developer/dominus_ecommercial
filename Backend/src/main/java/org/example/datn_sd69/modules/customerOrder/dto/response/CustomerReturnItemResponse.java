package org.example.datn_sd69.modules.customerOrder.dto.response;

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
        BigDecimal refundAmount,

        /**
         * 0 = Chờ xử lý
         * 1 = Đã chấp nhận
         * 2 = Từ chối
         * 3 = Đã hoàn tiền
         */
        Integer status,
        String statusText,
        String rejectReason
) {

    /**
     * Giữ constructor cũ để không làm vỡ các chỗ đang new CustomerReturnItemResponse(...)
     * chưa truyền trạng thái xử lý hoàn hàng.
     */
    public CustomerReturnItemResponse(
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
        this(
                orderItemId,
                productId,
                productVariantId,
                productName,
                brandName,
                sku,
                image,
                capacity,
                bottleType,
                orderedQuantity,
                returnQuantity,
                unitFinalPrice,
                itemAmount,
                voucherAllocatedAmount,
                refundAmount,
                null,
                null,
                null
        );
    }
}