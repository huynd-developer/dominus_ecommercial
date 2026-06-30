package org.example.datn_sd69.modules.pos.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductVariantPosResponse {

    private Integer variantId;
    private String sku;
    private String productName;
    private String brandName;
    private String capacityLabel;   // "100.0 ml"
    private String bottleTypeName;
    private BigDecimal price;
    private Integer stockQuantity;
    private String imageUrl;
}