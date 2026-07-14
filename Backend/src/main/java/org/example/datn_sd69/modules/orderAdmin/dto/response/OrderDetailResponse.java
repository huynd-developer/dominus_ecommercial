package org.example.datn_sd69.modules.orderAdmin.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class OrderDetailResponse {

    private Integer id;

    private String orderType;

    private String customerName;

    private String customerPhone;

    private String shippingAddress;

    private String paymentMethod;

    private Integer status;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal finalAmount;

    private String voucherName;

    private LocalDateTime createdAt;

    private List<OrderItemResponse> items;

}