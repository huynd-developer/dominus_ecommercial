package org.example.datn_sd69.modules.promotion.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class FlashSaleProductResponse {

    private Integer promotionId;

    private String promotionName;

    private LocalDateTime endDate;

    private Integer productVariantId;

    private Integer productId;

    private String productName;

    private String sku;

    private String capacity;

    private String bottleType;

    private BigDecimal originalPrice;

    private Double discountPercent;

    private BigDecimal salePrice;

    private Integer stockQuantity;
}