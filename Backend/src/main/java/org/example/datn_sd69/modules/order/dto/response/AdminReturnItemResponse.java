package org.example.datn_sd69.modules.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminReturnItemResponse {

    private Integer returnRequestItemId;

    private Integer orderItemId;

    private Integer productVariantId;

    private Integer productId;

    private String productName;

    private String brandName;

    private String sku;

    private String capacity;

    private String bottleType;

    private String imageUrl;

    private Integer orderedQuantity;

    private Integer returnQuantity;

    private BigDecimal unitFinalPrice;

    private BigDecimal itemAmount;

    private BigDecimal voucherAllocatedAmount;

    private BigDecimal refundAmount;

    /**
     * 0 = Chờ xử lý
     * 1 = Đã chấp nhận
     * 2 = Từ chối
     * 3 = Đã hoàn tiền
     */
    private Integer status;

    private String statusText;
}