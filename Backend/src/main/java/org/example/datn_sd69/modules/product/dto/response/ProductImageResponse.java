package org.example.datn_sd69.modules.product.dto.response;

import lombok.Data;

@Data
public class ProductImageResponse {

    private Integer id;

    private String imageUrl;

    private Boolean isPrimary;
}