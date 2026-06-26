package org.example.datn_sd69.modules.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductImageResponse {

    private Integer id;

    private String imageUrl;

    private Boolean primary;
}