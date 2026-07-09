package org.example.datn_sd69.modules.pos.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PosOrderResponse {

    private Integer orderId;

    // "COMPLETED" hoặc "PENDING_PAYMENT"
    private String status;

    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String paymentMethod;

    private String vnpayPaymentUrl;

    private LocalDateTime createdAt;

    private String customerName;
    private String customerPhone;
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
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
    }
}