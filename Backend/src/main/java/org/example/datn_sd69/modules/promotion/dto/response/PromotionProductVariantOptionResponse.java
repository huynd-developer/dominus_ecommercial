package org.example.datn_sd69.modules.promotion.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class PromotionProductVariantOptionResponse {

    private Integer productVariantId;

    private Integer productId;

    private String productName;

    private String sku;

    private String capacity;

    private String bottleType;

    private BigDecimal price;

    private Integer stockQuantity;

    private Integer status;

    private LocalDate manufacturingDate;

    private LocalDate expirationDate;

    private Boolean availableForPromotion;

    private String unavailableReason;
}