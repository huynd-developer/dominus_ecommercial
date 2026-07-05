package org.example.datn_sd69.modules.employee.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.example.datn_sd69.enums.RoleType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class EmployeeUpdateRequest {

    @NotNull(message = "Vai trò không được để trống")
    private RoleType role;

    @NotBlank(message = "Tên nhân viên không được để trống")
    @Size(max = 255, message = "Tên nhân viên không được vượt quá 255 ký tự")
    @Pattern(
            regexp = "^[\\p{L}\\s'.-]+$",
            message = "Tên nhân viên chỉ được chứa chữ cái và khoảng trắng"
    )
    private String name;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    private String email;

    /**
     * Khi sửa:
     * - null hoặc "" => giữ mật khẩu cũ
     * - có nhập => phải từ 6 đến 72 ký tự, không chứa khoảng trắng
     */
    @Pattern(
            regexp = "^$|^\\S{6,72}$",
            message = "Mật khẩu phải từ 6 đến 72 ký tự, không chứa khoảng trắng hoặc để trống nếu không đổi"
    )
    private String password;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(
            regexp = "^0\\d{9}$",
            message = "Số điện thoại phải có 10 số và bắt đầu bằng 0"
    )
    private String phone;

    @Size(max = 500, message = "Địa chỉ không được vượt quá 500 ký tự")
    private String address;

    @Size(max = 500, message = "Đường dẫn ảnh đại diện không được vượt quá 500 ký tự")
    private String avatarUrl;

    @NotBlank(message = "CCCD không được để trống")
    @Pattern(
            regexp = "^\\d{12}$",
            message = "CCCD phải gồm đúng 12 số"
    )
    private String citizenId;

    @NotBlank(message = "Chức vụ không được để trống")
    @Size(max = 100, message = "Chức vụ không được vượt quá 100 ký tự")
    private String jobTitle;

    @NotNull(message = "Lương không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Lương không được âm")
    @Digits(
            integer = 16,
            fraction = 2,
            message = "Lương tối đa 16 chữ số phần nguyên và 2 chữ số thập phân"
    )
    private BigDecimal salary;

    @NotNull(message = "Ngày vào làm không được để trống")
    @PastOrPresent(message = "Ngày vào làm không được lớn hơn ngày hiện tại")
    private LocalDate hireDate;
}