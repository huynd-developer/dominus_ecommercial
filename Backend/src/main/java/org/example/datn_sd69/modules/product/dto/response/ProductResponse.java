package org.example.datn_sd69.modules.product.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductResponse {

    private Integer id;

    private String name;

    private Integer brandId;
    private String brandName;

    private Integer categoryId;
    private String categoryName;

    private Integer concentrationId;
    private String concentrationName;

    private Integer gender;

    private Boolean isNiche;

    private String description;

    private Integer status;

    private LocalDateTime createdAt;

    private List<ProductImageResponse> images;

    private List<ProductVariantResponse> variants;
}