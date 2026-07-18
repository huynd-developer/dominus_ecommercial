package org.example.datn_sd69.modules.pos.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PosOrderResponse {

    private Integer orderId;

    /**
     * COMPLETED, PENDING_PAYMENT hoặc HELD
     */
    private String status;

    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;

    private String voucherCode;

    /**
     * Response trả cho FE:
     *
     * CASH
     * VNPAY
     * VIETQR
     * MIXED
     *
     * Với MIXED thì xem thêm transferProvider để biết:
     * - CASH + VNPAY
     * - CASH + VIETQR
     */
    private String paymentMethod;

    /**
     * Dùng khi paymentMethod = MIXED hoặc thanh toán chuyển khoản toàn phần.
     *
     * VNPAY:
     * - VNPay toàn phần hoặc CASH + VNPAY.
     *
     * VIETQR:
     * - VietQR toàn phần hoặc CASH + VIETQR.
     */
    private String transferProvider;

    /**
     * Tổng tiền đã ghi nhận ngay.
     *
     * CASH:
     * - paidAmount = finalAmount.
     *
     * VNPAY / VIETQR:
     * - paidAmount = 0 khi đang chờ thanh toán.
     * - remainingAmount = finalAmount.
     *
     * MIXED:
     * - paidAmount = cashGiven.
     * - remainingAmount = transferAmount.
     */
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;

    private BigDecimal cashGiven;
    private BigDecimal transferAmount;
    private BigDecimal changeAmount;

    /**
     * Link thanh toán VNPay.
     * Có dữ liệu khi:
     * - paymentMethod = VNPAY
     * - hoặc paymentMethod = MIXED và transferProvider = VNPAY
     */
    private String vnpayPaymentUrl;

    /**
     * Ảnh QR VietQR.
     * Có dữ liệu khi:
     * - paymentMethod = VIETQR
     * - hoặc paymentMethod = MIXED và transferProvider = VIETQR
     */
    private String vietQrImageUrl;

    /**
     * Nội dung chuyển khoản VietQR.
     * Ví dụ: POSDH123
     */
    private String vietQrContent;

    private LocalDateTime createdAt;

    private String customerName;
    private String customerPhone;
    private String customerEmail;

    private String cashierName;

    private Integer loyaltyPointsEarned;
    private Integer customerLoyaltyPointsAfter;

    private List<InvoiceItem> items;

    @Data
    @Builder
    public static class InvoiceItem {

        private String productName;
        private String sku;
        private String capacityLabel;
        private String bottleTypeName;

        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
    }
}