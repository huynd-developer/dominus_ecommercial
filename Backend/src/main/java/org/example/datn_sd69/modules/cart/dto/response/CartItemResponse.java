package org.example.datn_sd69.modules.cart.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemResponse {

    private Integer cartItemId;
    private Integer productVariantId;

    private String sku;
    private String productName;
    private String capacity;

    private Integer quantity;
    private BigDecimal price;
    private Integer stockQuantity;

    private String note;

    private String imageUrl;
    private String thumbnailUrl;

    private Boolean available;
    private String unavailableReason;
}