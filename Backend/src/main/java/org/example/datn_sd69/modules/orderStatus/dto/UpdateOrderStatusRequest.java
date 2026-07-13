package org.example.datn_sd69.modules.orderStatus.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(

        @NotNull(message = "Trạng thái đơn hàng không được để trống")
        @Min(value = 0, message = "Trạng thái đơn hàng không hợp lệ")
        @Max(value = 4, message = "Trạng thái đơn hàng không hợp lệ")
        Integer status
) {
}
