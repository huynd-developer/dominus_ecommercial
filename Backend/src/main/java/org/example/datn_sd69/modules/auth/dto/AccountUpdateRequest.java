package org.example.datn_sd69.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AccountUpdateRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên không vượt quá 100 ký tự")
    private String hoTen;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String soDienThoai;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Địa chỉ Email không đúng định dạng")
    private String email;

    private String diaChiMacDinh;

    // Các trường phục vụ logic đổi mật khẩu
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;
}
