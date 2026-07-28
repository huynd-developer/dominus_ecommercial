package org.example.datn_sd69.modules.order.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminOrderStatusUpdateRequest {

    @NotNull(message = "Trạng thái đơn hàng không được để trống")
    private Integer status;
}