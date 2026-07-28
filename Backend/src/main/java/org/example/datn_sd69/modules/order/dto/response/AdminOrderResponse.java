package org.example.datn_sd69.modules.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderResponse {

    private Integer orderId;

    private String orderCode;

    private String orderType;

    private Integer customerId;

    private String customerName;

    private String customerPhone;

    private String shippingAddress;

    private Integer cashierId;

    private VoucherInfo voucher;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal finalAmount;

    private String paymentMethod;

    /**
     * 0 = Chờ xác nhận
     * 1 = Đã xác nhận / Đang chuẩn bị hàng
     * 2 = Đang giao hàng
     * 3 = Hoàn thành
     * 4 = Đã hủy
     * 5 = Giao hàng thất bại
     * 6 = Yêu cầu hoàn hàng / đổi trả
     * 7 = Hoàn hàng / đổi trả hoàn tất
     */
    private Integer status;

    private String statusText;

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    private Integer totalQuantity;

    // --- BỔ SUNG CÁC TRƯỜNG DỮ LIỆU HOÀN HÀNG ---
    private String returnReason;
    private List<String> returnImages = new ArrayList<>();
    private List<String> returnVideos = new ArrayList<>();

    private List<AdminOrderItemResponse> items = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoucherInfo {
        private Integer voucherId;
        private String voucherCode;
        private String voucherName;
    }
}