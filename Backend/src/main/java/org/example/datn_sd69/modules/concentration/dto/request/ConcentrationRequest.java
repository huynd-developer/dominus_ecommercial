package org.example.datn_sd69.modules.concentration.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConcentrationRequest {

    @NotBlank(message = "Tên nồng độ không được để trống")
    @Size(max = 100, message = "Tên nồng độ không được vượt quá 100 ký tự")
    @Pattern(regexp = "^[\\p{L}\\s\\(\\)]+$", message = "Tên nồng độ chỉ được chứa chữ cái, khoảng trắng và dấu ngoặc đơn")
    private String name;

    @Min(value = 0, message = "Trạng thái không hợp lệ (chỉ nhận 0 hoặc 1)")
    @Max(value = 1, message = "Trạng thái không hợp lệ (chỉ nhận 0 hoặc 1)")
    private Integer status;
}