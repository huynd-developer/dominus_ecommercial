package org.example.datn_sd69.modules.pos.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Locale;

@Data
public class PosHeldOrderCheckoutRequest {

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    @Pattern(
            regexp = "^(CASH|VNPAY|VIETQR|MIXED|MIXED_VNPAY|MIXED_VIETQR)$",
            message = "Phương thức thanh toán chỉ được là CASH, VNPAY, VIETQR, MIXED, MIXED_VNPAY hoặc MIXED_VIETQR"
    )
    private String paymentMethod;

    @Pattern(
            regexp = "^(VNPAY|VIETQR)$",
            message = "Nhà cung cấp chuyển khoản chỉ được là VNPAY hoặc VIETQR"
    )
    private String transferProvider;

    @DecimalMin(value = "0.00", message = "Tiền mặt không được âm")
    private BigDecimal cashGiven;

    @DecimalMin(value = "0.00", message = "Tiền chuyển khoản không được âm")
    private BigDecimal transferAmount;

    /*
     * Snapshot của phiếu treo/pending mà FE đang nhìn thấy.
     * Chỉ dùng stale-check, không dùng để tính lại Order.
     */
    @DecimalMin(value = "0.00", message = "Tạm tính dự kiến không được âm")
    @Digits(integer = 18, fraction = 2, message = "Tạm tính dự kiến không hợp lệ")
    private BigDecimal expectedTotalAmount;

    @DecimalMin(value = "0.00", message = "Giảm giá dự kiến không được âm")
    @Digits(integer = 18, fraction = 2, message = "Giảm giá dự kiến không hợp lệ")
    private BigDecimal expectedDiscountAmount;

    @DecimalMin(value = "0.00", message = "Tổng thanh toán dự kiến không được âm")
    @Digits(integer = 18, fraction = 2, message = "Tổng thanh toán dự kiến không hợp lệ")
    private BigDecimal expectedFinalAmount;

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
