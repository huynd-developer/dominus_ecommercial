package org.example.datn_sd69.modules.pos.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Locale;

@Data
public class PosHoldRequest {

    @NotBlank(message = "Số điện thoại khách hàng không được để trống")
    @Pattern(
            regexp = "^(03|05|07|08|09)\\d{8}$",
            message = "Số điện thoại không hợp lệ. SĐT phải gồm 10 số và bắt đầu bằng 03, 05, 07, 08 hoặc 09."
    )
    private String customerPhone;

    @NotBlank(message = "Họ tên khách hàng không được để trống")
    @Size(min = 2, max = 100, message = "Họ tên khách hàng phải từ 2 đến 100 ký tự")
    private String customerName;

    @NotBlank(message = "Email khách hàng không được để trống")
    @Email(message = "Email khách hàng không đúng định dạng")
    @Size(max = 255, message = "Email khách hàng không được vượt quá 255 ký tự")
    private String customerEmail;

    @Size(max = 50, message = "Mã voucher không được vượt quá 50 ký tự")
    private String voucherCode;

    @Valid
    @NotEmpty(message = "Phiếu treo phải có ít nhất 1 sản phẩm")
    private List<PosItemRequest> items;

    public void setCustomerPhone(String customerPhone) {
        if (customerPhone == null || customerPhone.trim().isBlank()) {
            this.customerPhone = null;
            return;
        }

        this.customerPhone = customerPhone.replaceAll("[\\s.-]", "").trim();
    }

    public void setCustomerName(String customerName) {
        if (customerName == null || customerName.trim().isBlank()) {
            this.customerName = null;
            return;
        }

        this.customerName = customerName.trim().replaceAll("\\s+", " ");
    }

    public void setCustomerEmail(String customerEmail) {
        if (customerEmail == null || customerEmail.trim().isBlank()) {
            this.customerEmail = null;
            return;
        }

        this.customerEmail = customerEmail.trim().toLowerCase(Locale.ROOT);
    }

    public void setVoucherCode(String voucherCode) {
        if (voucherCode == null || voucherCode.trim().isBlank()) {
            this.voucherCode = null;
            return;
        }

        this.voucherCode = voucherCode.trim().toUpperCase(Locale.ROOT);
    }
}
