package org.example.datn_sd69.modules.capacity.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CapacityRequest {

    @NotNull(message = "Dung tích không được để trống")
    @Positive(message = "Dung tích phải lớn hơn 0")
    private Double value;

    @NotNull(message = "Trạng thái không được để trống")
    @Min(value = 0, message = "Trạng thái chỉ nhận giá trị 0 (Ẩn) hoặc 1 (Hiện)")
    @Max(value = 1, message = "Trạng thái chỉ nhận giá trị 0 (Ẩn) hoặc 1 (Hiện)")
    private Integer status;
}