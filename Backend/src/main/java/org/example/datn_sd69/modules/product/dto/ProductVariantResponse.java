package org.example.datn_sd69.modules.product.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductVariantResponse {

    private Integer id;

    private Integer productId;
    private String productName;

    private Integer capacityId;
    private Double capacityValue;

    private Integer bottleTypeId;
    private String bottleTypeName;

    private String sku;

    private BigDecimal price;

    private Integer stockQuantity;

    private Integer status;
}