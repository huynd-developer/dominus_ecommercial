package org.example.datn_sd69.modules.fragranceFamily.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FragranceFamilyRequest {

    @NotBlank(message = "Tên nhóm hương không được để trống")
    @Size(max = 255, message = "Tên nhóm hương không được vượt quá 255 ký tự")
    private String name;

    @Min(value = 0, message = "Trạng thái không hợp lệ (chỉ nhận 0 hoặc 1)")
    @Max(value = 1, message = "Trạng thái không hợp lệ (chỉ nhận 0 hoặc 1)")
    private Integer status;
}