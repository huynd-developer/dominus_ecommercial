package org.example.datn_sd69.modules.pos.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Locale;

@Data
public class PosHeldOrderCheckoutRequest {

    /**
     * CASH:
     * - Thanh toán tiền mặt toàn phần.
     *
     * VNPAY:
     * - Thanh toán VNPay toàn phần.
     *
     * VIETQR:
     * - Thanh toán VietQR toàn phần.
     *
     * MIXED:
     * - Tiền mặt + VNPAY hoặc VIETQR.
     * - Phân biệt bằng transferProvider.
     */
    @NotBlank(message = "Phương thức thanh toán không được để trống")
    @Pattern(
            regexp = "^(CASH|VNPAY|VIETQR|MIXED|MIXED_VNPAY|MIXED_VIETQR)$",
            message = "Phương thức thanh toán chỉ được là CASH, VNPAY, VIETQR, MIXED, MIXED_VNPAY hoặc MIXED_VIETQR"
    )
    private String paymentMethod;

    /**
     * Chỉ cần gửi khi paymentMethod = MIXED.
     *
     * MIXED + VNPAY:
     * transferProvider = VNPAY
     *
     * MIXED + VIETQR:
     * transferProvider = VIETQR
     */
    @Pattern(
            regexp = "^(VNPAY|VIETQR)$",
            message = "Nhà cung cấp chuyển khoản chỉ được là VNPAY hoặc VIETQR"
    )
    private String transferProvider;

    @DecimalMin(value = "0.00", message = "Tiền mặt không được âm")
    private BigDecimal cashGiven;

    @DecimalMin(value = "0.00", message = "Tiền chuyển khoản không được âm")
    private BigDecimal transferAmount;

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod == null
                ? null
                : paymentMethod.trim().toUpperCase(Locale.ROOT);
    }

    public void setTransferProvider(String transferProvider) {
        if (transferProvider == null || transferProvider.trim().isBlank()) {
            this.transferProvider = null;
            return;
        }

        this.transferProvider = transferProvider.trim().toUpperCase(Locale.ROOT);
    }
}