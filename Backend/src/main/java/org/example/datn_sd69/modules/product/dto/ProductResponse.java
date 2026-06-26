package org.example.datn_sd69.modules.product.dto;

import lombok.Data;

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

    private Integer status = 1;

    private List<ProductImageResponse> images;
}