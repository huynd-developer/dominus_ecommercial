package org.example.datn_sd69.modules.stockadjustment.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockAdjustmentItemRequest {

    /*
     * Giới hạn nghiệp vụ cho số lượng kiểm đếm của MỘT lô.
     * Giữ Integer để không thay đổi contract hiện tại của module.
     */
    public static final int MAX_ACTUAL_QUANTITY = 1_000_000;

    @NotNull(message = "Lô hàng không được để trống")
    @Positive(message = "Lô hàng phải lớn hơn 0")
    private Integer inventoryLotId;

    @NotNull(message = "Số lượng thực tế không được để trống")
    @Min(
            value = 0,
            message = "Số lượng thực tế phải lớn hơn hoặc bằng 0"
    )
    @Max(
            value = MAX_ACTUAL_QUANTITY,
            message = "Số lượng thực tế của mỗi lô không được vượt quá 1.000.000"
    )
    private Integer actualQuantity;

    /*
     * Lý do chỉ bắt buộc khi có chênh lệch.
     * Điều kiện bắt buộc phụ thuộc SystemQuantity trong DB nên được kiểm tra
     * tại StockAdjustmentServiceImpl, không dùng @NotBlank ở đây.
     */
    @Size(
            max = 500,
            message = "Lý do không được vượt quá 500 ký tự"
    )
    private String reason;
}