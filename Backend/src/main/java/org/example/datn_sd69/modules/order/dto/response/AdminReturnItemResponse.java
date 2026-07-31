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

    /**
     * Giá gốc của 1 sản phẩm tại thời điểm đặt hàng.
     */
    private BigDecimal unitOriginalPrice;

    /**
     * Số tiền giảm trên 1 sản phẩm tại thời điểm đặt hàng.
     */
    private BigDecimal unitDiscountAmount;

    /**
     * Giá sau giảm của 1 sản phẩm tại thời điểm đặt hàng.
     */
    private BigDecimal unitFinalPrice;

    /**
     * Tổng giá gốc theo số lượng hoàn = unitOriginalPrice * returnQuantity.
     */
    private BigDecimal itemOriginalAmount;

    /**
     * Tổng giảm giá theo số lượng hoàn = unitDiscountAmount * returnQuantity.
     */
    private BigDecimal itemDiscountAmount;

    /**
     * Tổng tiền sản phẩm sau giảm dòng hàng = unitFinalPrice * returnQuantity.
     */
    private BigDecimal itemAmount;

    /**
     * Phần voucher phân bổ vào dòng hoàn.
     */
    private BigDecimal voucherAllocatedAmount;

    /**
     * Số tiền thực tế cần hoàn cho dòng sản phẩm này.
     */
    private BigDecimal refundAmount;

    /**
     * 0 = Chờ xử lý
     * 1 = Đã chấp nhận
     * 2 = Từ chối hoàn hàng
     * 3 = Đã hoàn tiền
     */
    private Integer status;

    private String statusText;

    /**
     * Lý do admin từ chối sản phẩm hoàn.
     */
    private String rejectReason;
}