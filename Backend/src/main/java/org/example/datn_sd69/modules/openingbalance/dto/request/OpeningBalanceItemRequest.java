package org.example.datn_sd69.modules.openingbalance.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class OpeningBalanceItemRequest {

    @NotNull(message = "ProductVariantId không được để trống")
    @Positive(message = "ProductVariantId phải lớn hơn 0")
    private Integer productVariantId;

    @NotNull(message = "Số lượng thực tế không được để trống")
    @Positive(message = "Số lượng thực tế phải lớn hơn 0")
    private Integer quantity;

    private LocalDate manufacturedDate;

    @NotNull(message = "Ngày nhận không được để trống")
    private LocalDate receivedDate;

    @NotNull(message = "Hạn sử dụng không được để trống")
    private LocalDate expirationDate;

    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String note;
}