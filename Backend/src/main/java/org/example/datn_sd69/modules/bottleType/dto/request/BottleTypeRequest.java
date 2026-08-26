package org.example.datn_sd69.modules.bottleType.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BottleTypeRequest {

    @NotBlank(message = "Tên loại chai không được để trống")
    @Size(max = 50, message = "Tên loại chai không được vượt quá 50 ký tự")
    // ĐÃ SỬA: Đẩy dấu \- xuống cuối cùng và đổi \d thành 0-9 để Java không bị lỗi biên dịch
    @Pattern(regexp = "^[\\p{L}\\p{M}0-9\\s()._\\-]+$", message = "Tên loại chai chỉ được chứa chữ cái, số, khoảng trắng và các ký tự: -, _, (), .")
    private String name;

    @NotNull(message = "Trạng thái không được để trống")
    @Min(value = 0, message = "Trạng thái chỉ nhận giá trị 0 (Ẩn) hoặc 1 (Hiện)")
    @Max(value = 1, message = "Trạng thái chỉ nhận giá trị 0 (Ẩn) hoặc 1 (Hiện)")
    private Integer status;
}