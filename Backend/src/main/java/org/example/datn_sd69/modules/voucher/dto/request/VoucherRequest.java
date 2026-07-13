package org.example.datn_sd69.modules.voucher.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class VoucherRequest {
    @NotBlank(message = "Mã voucher không được để trống")
    private String code;

    @NotBlank(message = "Kiểu giảm giá không được để trống")
    private String discountType; // PERCENT hoặc FIXED

    @NotNull(message = "Mức giảm không được để trống")
    @Min(value = 0, message = "Mức giảm phải lớn hơn hoặc bằng 0")
    private BigDecimal discountValue;

    @Min(value = 0, message = "Đơn tối thiểu phải lớn hơn hoặc bằng 0")
    private BigDecimal minOrderValue;

    @Min(value = 0, message = "Mức giảm tối đa phải lớn hơn hoặc bằng 0")
    private BigDecimal maxDiscount;

    @NotNull(message = "Giới hạn lượt dùng không được để trống")
    @Min(value = 1, message = "Lượt dùng phải lớn hơn 0")
    private Integer usageLimit;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDateTime startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDateTime endDate;

    private Integer status;
}