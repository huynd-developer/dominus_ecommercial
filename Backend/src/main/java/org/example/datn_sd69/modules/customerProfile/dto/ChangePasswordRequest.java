package org.example.datn_sd69.modules.customerProfile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(

        @NotBlank(message = "Mật khẩu cũ không được để trống")
        @Size(min = 8, max = 50, message = "Mật khẩu cũ phải từ 8 đến 50 ký tự")
        String oldPassword,

        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(min = 8, max = 50, message = "Mật khẩu mới phải từ 8 đến 50 ký tự")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.#])[A-Za-z\\d@$!%*?&.#]{8,50}$",
                message = "Mật khẩu mới phải có chữ hoa, chữ thường, số, ít nhất 1 ký tự đặc biệt trong @$!%*?&.# và không chứa khoảng trắng"
        )
        String newPassword,

        @NotBlank(message = "Xác nhận mật khẩu không được để trống")
        @Size(min = 8, max = 50, message = "Xác nhận mật khẩu phải từ 8 đến 50 ký tự")
        String confirmPassword
) {
}