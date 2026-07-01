package org.example.datn_sd69.modules.brand.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrandRequest {

    @NotBlank(message = "Tên thương hiệu không được để trống!")
    @Size(max = 255, message = "Tên thương hiệu không được vượt quá 255 ký tự!")
    private String name;

    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự!")
    private String description;

    @Size(max = 500, message = "Đường dẫn ảnh không được vượt quá 500 ký tự!")
    private String logoUrl; // Thêm trường này để nhận link ảnh

    @NotNull(message = "Trạng thái không được để trống!")
    @Min(value = 0, message = "Trạng thái không hợp lệ! Chỉ nhận giá trị 0 (Ẩn) hoặc 1 (Hiện).")
    @Max(value = 1, message = "Trạng thái không hợp lệ! Chỉ nhận giá trị 0 (Ẩn) hoặc 1 (Hiện).")
    private Integer status;
}