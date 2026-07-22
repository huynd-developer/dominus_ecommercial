package org.example.datn_sd69.modules.orderAdmin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponse {

    /**
     * Mã đơn hàng
     */
    private Integer id;

    /**
     * ONLINE / IN_STORE
     */
    private String orderType;

    /**
     * Khách hàng
     */
    private Integer customerId;

    private String customerName;

    private String customerPhone;

    private String shippingAddress;

    /**
     * Thu ngân
     */
    private Integer cashierId;

    private String cashierName;

    /**
     * Voucher
     */
    private Integer voucherId;

    private String voucherCode;

    /**
     * Thanh toán
     */
    private String paymentMethod;

    /**
     * Tổng tiền hàng
     */
    private BigDecimal totalAmount;

    /**
     * Tiền giảm
     */
    private BigDecimal discountAmount;

    /**
     * Thành tiền
     */
    private BigDecimal finalAmount;

    /**
     * Trạng thái
     */
    private Integer status;

    /**
     * Tên trạng thái
     */
    private String statusName;

    /**
     * Thời gian tạo
     */
    private LocalDateTime createdAt;

    /**
     * Thời gian hoàn thành
     */
    private LocalDateTime completedAt;

    /**
     * Điểm thưởng đã cộng
     */
    private Integer loyaltyPointsEarned;

    /**
     * Đã cộng điểm hay chưa
     */
    private Boolean loyaltyPointsApplied;

    /**
     * Danh sách sản phẩm
     */
    private List<OrderItemResponse> items = new ArrayList<>();

}