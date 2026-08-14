package org.example.datn_sd69.modules.stockadjustment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockAdjustmentItemRequest {

    @NotNull(message = "Lô hàng không được để trống")
    private Integer inventoryLotId;

    @NotNull(message = "Số lượng thực tế không được để trống")
    @Min(
            value = 0,
            message = "Số lượng thực tế phải lớn hơn hoặc bằng 0"
    )
    private Integer actualQuantity;

    @Size(
            max = 500,
            message = "Lý do không được vượt quá 500 ký tự"
    )
    private String reason;
}
