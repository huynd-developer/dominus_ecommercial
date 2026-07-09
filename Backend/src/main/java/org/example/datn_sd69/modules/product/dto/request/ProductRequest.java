package org.example.datn_sd69.modules.product.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductRequest {
    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    @NotNull
    private Integer brandId;

    @NotNull
    private Integer categoryId;

    @NotNull
    private Integer concentrationId;

    @NotNull
    private Integer gender;

    @NotNull
    private Boolean isNiche;

    @NotNull
    private Integer status;
}