package org.example.datn_sd69.modules.brand.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrandRequest {

    @NotBlank(message = "Tên thương hiệu không được để trống!")
    @Size(max = 255, message = "Tên thương hiệu không được vượt quá 255 ký tự!")
    // 👇 BỔ SUNG: Cho phép chữ, số, khoảng trắng và các ký tự đặc biệt của thương hiệu (&, -, ., ')
    @Pattern(regexp = "^[\\p{L}\\d\\s&'\\.\\-]+$", message = "Tên thương hiệu chỉ được chứa chữ cái, số, khoảng trắng và các ký tự: &, -, ., '")
    private String name;

    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự!")
    private String description;

    @Size(max = 500, message = "Đường dẫn ảnh không được vượt quá 500 ký tự!")
    // 👇 BỔ SUNG: Chặn trường hợp nhập link ảnh chứa ký tự nguy hiểm (XSS script) nếu có nhập
    @Pattern(regexp = "^[^<>\"']*$", message = "Đường dẫn ảnh không được chứa các ký tự mã độc <, >, \", '")
    private String logoUrl;

    @NotNull(message = "Trạng thái không được để trống!")
    @Min(value = 0, message = "Trạng thái không hợp lệ! Chỉ nhận giá trị 0 (Ẩn) hoặc 1 (Hiện).")
    @Max(value = 1, message = "Trạng thái không hợp lệ! Chỉ nhận giá trị 0 (Ẩn) hoặc 1 (Hiện).")
    private Integer status;
}