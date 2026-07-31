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
     * 7 = Hoàn hàng / đổi trả hoàn tất / đã xử lý hoàn tiền
     */
    private Integer status;

    private String statusText;

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    /**
     * Lý do hủy đơn. Có thể do khách chọn hoặc admin nhập/chọn khi hủy.
     */
    private String cancelReason;

    /**
     * Thời điểm hủy đơn.
     */
    private LocalDateTime cancelledAt;

    private Integer totalQuantity;

    private Boolean isPaymentReported;

    // --- THÔNG TIN HOÀN HÀNG / HOÀN TIỀN ---
    private String returnType;

    private String returnReason;

    private String returnDescription;

    private String returnEmail;

    private LocalDateTime returnRequestedAt;

    private String refundMethod;

    private BigDecimal returnRefundAmount;

    private BigDecimal refundAmount;

    private String bankName;

    private String bankAccountNumber;

    private String bankAccountHolder;

    /**
     * PENDING = Chờ xử lý
     * ACCEPTED = Đã chấp nhận / chờ hoàn tiền
     * REJECTED = Từ chối hoàn hàng
     * REFUNDED = Đã xử lý hoàn tiền
     * PARTIAL = Có nhiều trạng thái item khác nhau
     */
    private String returnProcessStatus;

    private String returnProcessStatusText;

    /**
     * Lý do admin từ chối hoàn hàng.
     * Khách hàng cần thấy field này ở lịch sử đơn hàng.
     */
    private String returnRejectReason;

    private Boolean canAcceptReturn;

    private Boolean canRejectReturn;

    /**
     * true khi đơn đang ở trạng thái 6 và tất cả sản phẩm hoàn đã được chấp nhận.
     */
    private Boolean canMarkReturnRefunded;

    private List<String> returnImages = new ArrayList<>();

    private List<String> returnVideos = new ArrayList<>();

    /**
     * Danh sách sản phẩm khách yêu cầu hoàn.
     * Chỉ cần dùng ở màn chi tiết đơn hàng.
     */
    private List<AdminReturnItemResponse> returnItems = new ArrayList<>();

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