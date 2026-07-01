package org.example.datn_sd69.modules.product.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductVariantResponse {

    /**
     * Id Variant
     */
    private Integer id;

    /**
     * Product
     */
    private Integer productId;

    private String productName;

    /**
     * Capacity
     */
    private Integer capacityId;

    private Double capacityValue;

    /**
     * BottleType
     */
    private Integer bottleTypeId;

    private String bottleTypeName;

    /**
     * SKU
     */
    private String sku;

    /**
     * Giá
     */
    private BigDecimal price;

    /**
     * Tồn kho
     */
    private Integer stockQuantity;

    /**
     * Trạng thái
     */
    private Integer status;

}