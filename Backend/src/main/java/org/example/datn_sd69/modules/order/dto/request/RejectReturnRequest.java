package org.example.datn_sd69.modules.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectReturnRequest(

        @NotBlank(message = "Vui lòng nhập lý do từ chối hoàn hàng")
        @Size(min = 5, max = 500, message = "Lý do từ chối phải từ 5 đến 500 ký tự")
        String reason
) {
}