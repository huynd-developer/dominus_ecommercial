package org.example.datn_sd69.modules.voucher.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class VoucherRequest {
    private String code; // Để trống để backend tự sinh

    @NotBlank(message = "Kiểu giảm giá không được để trống")
    private String discountType;

    @NotNull(message = "Mức giảm không được để trống")
    @DecimalMin(value = "0", message = "Mức giảm phải >= 0")
    @DecimalMax(value = "100000000", message = "Mức giảm không được vượt quá 100 triệu")
    private BigDecimal discountValue;

    @DecimalMin(value = "0", message = "Đơn tối thiểu phải >= 0")
    @DecimalMax(value = "100000000", message = "Đơn tối thiểu không được quá 100 triệu")
    private BigDecimal minOrderValue;

    @DecimalMin(value = "0", message = "Mức giảm tối đa phải >= 0")
    @DecimalMax(value = "100000000", message = "Mức giảm tối đa không được quá 100 triệu")
    private BigDecimal maxDiscount;

    @NotNull(message = "Giới hạn lượt dùng không được để trống")
    @Min(value = 1, message = "Lượt dùng phải > 0")
    private Integer usageLimit;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDateTime startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDateTime endDate;

    private Integer status;
}