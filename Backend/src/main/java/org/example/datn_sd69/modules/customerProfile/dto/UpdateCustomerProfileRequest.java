package org.example.datn_sd69.modules.customerProfile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateCustomerProfileRequest(

        @NotBlank(message = "Họ tên không được để trống")
        @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
        @Pattern(
                regexp = "^[\\p{L}]+(?: [\\p{L}]+)*$",
                message = "Họ tên chỉ được chứa chữ cái và một khoảng trắng giữa các từ"
        )
        String name,

        @NotBlank(message = "Số điện thoại không được để trống")
        @Pattern(
                regexp = "^0\\d{9}$",
                message = "Số điện thoại phải gồm 10 số và bắt đầu bằng 0"
        )
        String phone,

        @NotBlank(message = "Địa chỉ không được để trống")
        @Size(min = 5, max = 500, message = "Địa chỉ phải từ 5 đến 500 ký tự")
        String address,

        LocalDate dateOfBirth,

        Integer gender
) {
}