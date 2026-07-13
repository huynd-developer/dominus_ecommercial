package org.example.datn_sd69.modules.orderAdmin.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderResponse {

    /**
     * Danh sách đơn hàng
     */
    @Data
    public static class ListItem {

        private Integer orderId;

        private String orderType; // ONLINE | IN_STORE

        private String customerName;

        private String customerPhone;

        private BigDecimal finalAmount;

        private Integer status;

        private String statusName;

        private LocalDateTime createdAt;
    }

    /**
     * Chi tiết sản phẩm trong đơn
     */
    @Data
    public static class OrderItem {

        private Integer productVariantId;

        private String productName;

        private Integer quantity;

        private BigDecimal originalPrice;

        private BigDecimal discountAmount;

        private BigDecimal finalPrice;

        private String image;
    }

    /**
     * Chi tiết đơn hàng
     */
    @Data
    public static class Detail {

        private Integer orderId;

        private String orderType;

        private String customerName;

        private String customerPhone;

        private String shippingAddress;

        private String paymentMethod;

        private Integer status;

        private String statusName;

        private BigDecimal totalAmount;

        private BigDecimal discountAmount;

        private BigDecimal finalAmount;

        private LocalDateTime createdAt;

        private List<OrderItem> items;
    }

    /**
     * Response khi đổi trạng thái hoặc hủy đơn
     */
    @Data
    public static class UpdateStatus {

        private Integer orderId;

        private Integer oldStatus;

        private Integer newStatus;

        private String message;
    }
}