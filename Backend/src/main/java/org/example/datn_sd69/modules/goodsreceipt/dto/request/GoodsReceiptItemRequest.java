package org.example.datn_sd69.modules.goodsreceipt.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class GoodsReceiptItemRequest {

    @NotNull(message = "ProductVariantId không được để trống")
    @Positive(message = "ProductVariantId phải lớn hơn 0")
    private Integer productVariantId;

    @NotBlank(message = "Mã lô không được để trống")
    @Size(max = 100, message = "Mã lô không được vượt quá 100 ký tự")
    private String lotCode;

    @NotNull(message = "Số lượng không được để trống")
    @Positive(message = "Số lượng phải lớn hơn 0")
    private Integer quantity;

    @DecimalMin(value = "0.00", inclusive = true, message = "Đơn giá nhập phải lớn hơn hoặc bằng 0")
    @Digits(integer = 16, fraction = 2, message = "Đơn giá nhập không đúng định dạng DECIMAL(18,2)")
    private BigDecimal unitCost;

    private LocalDate manufacturedDate;

    @NotNull(message = "Ngày nhận hàng không được để trống")
    private LocalDate receivedDate;

    @NotNull(message = "Hạn sử dụng không được để trống")
    private LocalDate expirationDate;

    @Size(max = 500, message = "Ghi chú dòng không được vượt quá 500 ký tự")
    private String note;
}
