package org.example.datn_sd69.modules.pos.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Locale;

@Data
public class PosSaveCustomerRequest {

    @NotBlank(message = "Số điện thoại khách hàng không được để trống")
    @Pattern(
            regexp = "^(03|05|07|08|09)\\d{8}$",
            message = "Số điện thoại không hợp lệ. SĐT phải gồm 10 số và bắt đầu bằng 03, 05, 07, 08 hoặc 09."
    )
    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
    private String phone;

    @NotBlank(message = "Họ tên khách hàng không được để trống")
    @Size(min = 2, max = 100, message = "Họ tên khách hàng phải từ 2 đến 100 ký tự")
    @Pattern(
            regexp = "^[\\p{L}]+(?:\\s+[\\p{L}]+)*$",
            message = "Họ tên chỉ được chứa chữ cái và khoảng trắng, không được chứa số hoặc ký tự đặc biệt."
    )
    private String name;

    @NotBlank(message = "Email khách hàng không được để trống")
    @Email(message = "Email khách hàng không đúng định dạng")
    @Pattern(
            regexp = "^(?!.*\\.\\.)(?!\\.)(?!.*\\.@)[A-Za-z0-9_%+-]+(?:\\.[A-Za-z0-9_%+-]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,}$",
            message = "Email khách hàng không đúng định dạng"
    )
    @Size(max = 255, message = "Email khách hàng không được vượt quá 255 ký tự")
    private String email;

    public void setPhone(String phone) {
        if (phone == null || phone.trim().isBlank()) {
            this.phone = null;
            return;
        }

        this.phone = phone.replaceAll("[\\s.-]", "").trim();
    }

    public void setName(String name) {
        if (name == null || name.trim().isBlank()) {
            this.name = null;
            return;
        }

        this.name = name.trim().replaceAll("\\s+", " ");
    }

    public void setEmail(String email) {
        if (email == null || email.trim().isBlank()) {
            this.email = null;
            return;
        }

        this.email = email.trim().toLowerCase(Locale.ROOT);
    }
}