package org.example.datn_sd69.modules.orderAdmin.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderItemResponse {

    private Integer id;

    private Integer productVariantId;

    private String productName;

    private String sku;

    private String capacity;

    private String bottleType;

    private Integer quantity;

    private BigDecimal originalPrice;

    private BigDecimal discountAmount;

    private BigDecimal finalPrice;

    private String image;

    private String note;

}