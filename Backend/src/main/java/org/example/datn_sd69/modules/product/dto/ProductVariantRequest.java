package org.example.datn_sd69.modules.product.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductVariantRequest {

    @NotNull(message = "Vui lòng chọn dung tích")
    private Integer capacityId;

    @NotNull(message = "Vui lòng chọn loại chai")
    private Integer bottleTypeId;

    private String sku;

    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal price;

    @NotNull(message = "Số lượng không được để trống")
    @Min(0)
    private Integer stockQuantity;

    @NotNull(message = "Vui lòng chọn trạng thái")
    @Min(0)
    @Max(1)
    private Integer status;
}