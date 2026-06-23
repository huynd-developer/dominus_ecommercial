package org.example.datn_sd69.modules.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class DanhGiaRequest {

    @NotNull(message = "Không xác định được sản phẩm cần đánh giá.")
    private Long chiTietDonHangId;

    @NotNull(message = "Vui lòng chọn số sao.")
    @Min(value = 1, message = "Đánh giá tối thiểu là 1 sao.")
    @Max(value = 5, message = "Đánh giá tối đa là 5 sao.")
    private Integer diemSoSao;

    private String binhLuan;
}
