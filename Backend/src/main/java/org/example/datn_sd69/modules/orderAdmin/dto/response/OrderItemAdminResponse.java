package org.example.datn_sd69.modules.orderAdmin.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemAdminResponse {
    private Integer id;
    private String productName;
    private String capacity;
    private String bottleType;
    private Integer quantity;
    private BigDecimal originalPrice;
    private BigDecimal finalPrice;
    private BigDecimal discountAmount;
    private String image;
}