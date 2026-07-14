package org.example.datn_sd69.modules.promotion.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PromotionStatusRequest {

    @NotNull(message = "Trạng thái khuyến mãi không được để trống")
    @Min(value = 0, message = "Trạng thái chỉ được là 0 hoặc 1")
    @Max(value = 1, message = "Trạng thái chỉ được là 0 hoặc 1")
    private Integer status;
}