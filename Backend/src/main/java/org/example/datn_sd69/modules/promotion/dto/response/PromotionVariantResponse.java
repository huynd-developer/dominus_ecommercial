package org.example.datn_sd69.modules.promotion.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PromotionVariantResponse {

    private Integer productVariantId;

    private String sku;

    private String productName;

    private String capacity;

    private String bottleType;

    private BigDecimal originalPrice;

    private Double discountPercent;

    private BigDecimal salePrice;

    private Integer stockQuantity;
}