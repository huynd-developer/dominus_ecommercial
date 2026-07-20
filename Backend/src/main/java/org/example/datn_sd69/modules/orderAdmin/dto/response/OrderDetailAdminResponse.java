package org.example.datn_sd69.modules.orderAdmin.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderDetailAdminResponse {
    private Integer id;
    private String customerName;
    private String customerPhone;
    private String shippingAddress;
    private String orderType;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String paymentMethod;
    private Integer status;
    private LocalDateTime createdAt;

    // Danh sách sản phẩm
    private List<OrderItemAdminResponse> items;
}