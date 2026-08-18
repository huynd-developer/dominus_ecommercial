package org.example.datn_sd69.modules.stockadjustment.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StockAdjustmentSaveRequest {

    @Size(
            max = 1000,
            message = "Ghi chú không được vượt quá 1000 ký tự"
    )
    private String note;

    @Valid
    @NotEmpty(
            message = "Phiếu kiểm kê phải có ít nhất một lô hàng"
    )
    private List<StockAdjustmentItemRequest> items;
}
