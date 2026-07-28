package org.example.datn_sd69.modules.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderItemResponse {

    private Integer orderItemId;

    private Integer productVariantId;

    private String sku;

    private String productName;

    private String capacity;

    private String bottleType;

    private Integer quantity;

    private BigDecimal originalPrice;

    private BigDecimal discountAmount;

    private BigDecimal finalPrice;

    private BigDecimal lineTotal;

    private String note;

    private String imageUrl;
}