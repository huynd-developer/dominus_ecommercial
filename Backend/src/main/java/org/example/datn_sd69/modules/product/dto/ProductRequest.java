package org.example.datn_sd69.modules.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    @NotNull(message = "Brand không được để trống")
    private Integer brandId;

    @NotNull(message = "Category không được để trống")
    private Integer categoryId;

    @NotNull(message = "Concentration không được để trống")
    private Integer concentrationId;

    @NotNull(message = "Gender không được để trống")
    private Integer gender;

    @NotNull(message = "IsNiche không được để trống")
    private Boolean isNiche;

    private String description;

    private Integer status;
}