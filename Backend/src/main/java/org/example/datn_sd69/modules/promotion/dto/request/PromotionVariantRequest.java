package org.example.datn_sd69.modules.promotion.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PromotionVariantRequest {

    @NotNull(message = "Biến thể sản phẩm không được để trống")
    @Positive(message = "ID biến thể sản phẩm phải lớn hơn 0")
    private Integer productVariantId;

    @NotNull(message = "Phần trăm giảm giá không được để trống")
    @DecimalMin(value = "0.01", message = "Phần trăm giảm giá phải lớn hơn 0")
    @DecimalMax(value = "99.99", message = "Phần trăm giảm giá phải nhỏ hơn 100")
    private Double discountPercent;
}