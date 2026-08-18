package org.example.datn_sd69.modules.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryConfigUpdateRequest {

    @NotNull(message = "Số ngày cảnh báo không được để trống")
    @Min(
            value = 0,
            message = "Số ngày cảnh báo phải từ 0 đến 3650 ngày"
    )
    @Max(
            value = 3650,
            message = "Số ngày cảnh báo phải từ 0 đến 3650 ngày"
    )
    private Short expiryWarningDays;
}