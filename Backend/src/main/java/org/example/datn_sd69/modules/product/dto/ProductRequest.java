package org.example.datn_sd69.modules.product.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 255, message = "Tên sản phẩm tối đa 255 ký tự")
    private String name;

    @NotNull(message = "Vui lòng chọn thương hiệu")
    private Integer brandId;

    @NotNull(message = "Vui lòng chọn danh mục")
    private Integer categoryId;

    @NotNull(message = "Vui lòng chọn nồng độ")
    private Integer concentrationId;

    @Size(max = 2000,
            message = "Mô tả tối đa 2000 ký tự")
    private String description;

    @NotNull(message = "Vui lòng chọn giới tính")
    private Integer gender;

    @NotNull(message = "Vui lòng chọn loại nước hoa")
    private Boolean isNiche;

    @NotNull(message = "Vui lòng chọn trạng thái")
    @Min(value = 0)
    @Max(value = 1)
    private Integer status;

}