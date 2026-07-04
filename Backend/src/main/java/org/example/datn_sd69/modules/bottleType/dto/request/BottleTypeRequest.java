package org.example.datn_sd69.modules.bottleType.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BottleTypeRequest {

    @NotBlank(message = "Tên loại chai không được để trống")
    @Size(max = 255, message = "Tên loại chai không được vượt quá 255 ký tự")
    private String name;

    @NotNull(message = "Trạng thái không được để trống")
    @Min(value = 0, message = "Trạng thái chỉ nhận giá trị 0 (Ẩn) hoặc 1 (Hiện)")
    @Max(value = 1, message = "Trạng thái chỉ nhận giá trị 0 (Ẩn) hoặc 1 (Hiện)")
    private Integer status;
}