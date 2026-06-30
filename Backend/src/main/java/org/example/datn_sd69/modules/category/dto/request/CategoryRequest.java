package org.example.datn_sd69.modules.category.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryRequest {

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 255, message = "Tên danh mục không được vượt quá 255 ký tự")
    private String name;

    @NotNull(message = "Trạng thái không được để trống")
    @Min(value = 0, message = "Trạng thái chỉ nhận giá trị 0 (Ẩn) hoặc 1 (Hiện)")
    @Max(value = 1, message = "Trạng thái chỉ nhận giá trị 0 (Ẩn) hoặc 1 (Hiện)")
    private Integer status;
}
