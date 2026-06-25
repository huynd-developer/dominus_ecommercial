package org.example.datn_sd69.modules.auth.cart.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "Vui lòng nhập Email")
    private String email;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    private String password;
}