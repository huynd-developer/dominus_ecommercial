package org.example.datn_sd69.modules.pos.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PosSaveCustomerRequest {

    @NotBlank(message = "Số điện thoại khách hàng không được để trống")
    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
    private String phone;

    @NotBlank(message = "Họ tên khách hàng không được để trống")
    @Size(min = 2, max = 100, message = "Họ tên khách hàng phải từ 2 đến 100 ký tự")
    private String name;

    @NotBlank(message = "Email khách hàng không được để trống")
    @Email(message = "Email khách hàng không đúng định dạng")
    @Size(max = 255, message = "Email khách hàng không được vượt quá 255 ký tự")
    private String email;
}