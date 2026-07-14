package org.example.datn_sd69.modules.pos.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Data
public class PosCheckoutRequest {

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    @Pattern(
            regexp = "^(CASH|VNPAY|MIXED)$",
            message = "Phương thức thanh toán chỉ được là CASH, VNPAY hoặc MIXED"
    )
    private String paymentMethod;

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

    /**
     * Tiền mặt khách đưa.
     *
     * CASH:
     * - cashGiven >= finalAmount.
     *
     * MIXED:
     * - cashGiven là phần tiền mặt dùng để thanh toán.
     * - Không xử lý tiền thừa trong MIXED. Nếu khách đưa dư, thu ngân trả lại ngay và chỉ nhập số tiền thực nhận vào đơn.
     */
    @DecimalMin(value = "0.00", message = "Tiền mặt không được âm")
    private BigDecimal cashGiven;

    /**
     * Tiền chuyển khoản / VNPay.
     *
     * VNPAY:
     * - Nếu FE không gửi, BE tự hiểu là thanh toán toàn bộ finalAmount bằng VNPay.
     *
     * MIXED:
     * - transferAmount là phần còn lại cần thanh toán bằng VNPay.
     */
    @DecimalMin(value = "0.00", message = "Tiền chuyển khoản không được âm")
    private BigDecimal transferAmount;

    @Valid
    @NotEmpty(message = "Hóa đơn phải có ít nhất 1 sản phẩm")
    private List<PosItemRequest> items;

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod == null
                ? null
                : paymentMethod.trim().toUpperCase(Locale.ROOT);
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