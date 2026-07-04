package org.example.datn_sd69.modules.pos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PosCheckoutRequest {

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    @Pattern(
            regexp = "^(CASH|VNPAY)$",
            message = "Phương thức thanh toán chỉ được là CASH hoặc VNPAY"
    )
    private String paymentMethod;

    /**
     * Null = khách vãng lai.
     * Có SĐT thì bắt buộc đúng định dạng Việt Nam.
     */
    @Pattern(
            regexp = "^(03|05|07|08|09)\\d{8}$",
            message = "Số điện thoại không hợp lệ. SĐT phải gồm 10 số và bắt đầu bằng 03, 05, 07, 08 hoặc 09."
    )
    private String customerPhone;

    @Size(max = 100, message = "Tên khách hàng không được vượt quá 100 ký tự")
    private String customerName;

    @Size(max = 50, message = "Mã voucher không được vượt quá 50 ký tự")
    private String voucherCode;

    @DecimalMin(value = "0.00", message = "Tiền khách đưa không được âm")
    private BigDecimal cashGiven;

    @DecimalMin(value = "0.00", message = "Tiền thừa không được âm")
    private BigDecimal changeAmount;

    @Valid
    @NotEmpty(message = "Hóa đơn phải có ít nhất 1 sản phẩm")
    private List<PosItemRequest> items;

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod == null ? null : paymentMethod.trim().toUpperCase();
    }

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

        this.customerName = customerName.trim();
    }

    public void setVoucherCode(String voucherCode) {
        if (voucherCode == null || voucherCode.trim().isBlank()) {
            this.voucherCode = null;
            return;
        }

        this.voucherCode = voucherCode.trim();
    }
}