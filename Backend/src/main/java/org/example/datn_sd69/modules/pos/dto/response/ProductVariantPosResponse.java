package org.example.datn_sd69.modules.pos.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ProductVariantPosResponse {

    private Integer variantId;
    private String sku;

    private String productName;
    private String brandName;

    private String capacityLabel;
    private String bottleTypeName;

    private BigDecimal price;
    private Integer stockQuantity;

    private LocalDate manufacturingDate;
    private LocalDate expirationDate;

    private Integer status;
    private Boolean expired;
    private Boolean sellable;
    private String unavailableReason;

    private String imageUrl;
}