package org.example.datn_sd69.modules.capacity.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CapacityRequest {

    @NotNull(message = "Dung tích không được để trống")
    @DecimalMin(value = "0.01", message = "Dung tích phải lớn hơn 0")
    @DecimalMax(value = "5000", message = "Dung tích không hợp lệ, không được vượt quá 5000 ml")
    private Double value;

    @NotNull(message = "Trạng thái không được để trống")
    @Min(value = 0, message = "Trạng thái chỉ nhận giá trị 0 (Ẩn) hoặc 1 (Hiện)")
    @Max(value = 1, message = "Trạng thái chỉ nhận giá trị 0 (Ẩn) hoặc 1 (Hiện)")
    private Integer status;
}