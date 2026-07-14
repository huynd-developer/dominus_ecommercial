package org.example.datn_sd69.modules.fragranceFamily.dto.request;

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
public class FragranceFamilyRequest {

    @NotBlank(message = "Tên nhóm hương không được để trống")
    @Size(max = 255, message = "Tên nhóm hương không được vượt quá 255 ký tự")
    // 👇 Đã sửa lại đúng chuẩn Regex của Java (Chỉ dùng 2 dấu gạch chéo \\)
    @Pattern(regexp = "^[\\p{L}\\s()]+$", message = "Tên nhóm hương chỉ được chứa chữ cái, khoảng trắng và dấu ngoặc đơn")
    private String name;

    // 👇 Bổ sung NotNull để chặn giá trị rỗng
    @NotNull(message = "Trạng thái không được để trống")
    @Min(value = 0, message = "Trạng thái không hợp lệ (chỉ nhận 0 hoặc 1)")
    @Max(value = 1, message = "Trạng thái không hợp lệ (chỉ nhận 0 hoặc 1)")
    private Integer status;
}