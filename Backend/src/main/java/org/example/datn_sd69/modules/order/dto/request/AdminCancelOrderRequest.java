package org.example.datn_sd69.modules.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCancelOrderRequest(
        @NotBlank(message = "Vui lòng chọn lý do hủy đơn")
        @Size(max = 100, message = "Lý do hủy không được vượt quá 100 ký tự")
        String reason,

        @Size(max = 180, message = "Mô tả hủy không được vượt quá 180 ký tự")
        String description
) {
}