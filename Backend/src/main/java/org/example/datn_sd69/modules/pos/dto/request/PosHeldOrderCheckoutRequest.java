package org.example.datn_sd69.modules.pos.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Locale;

@Data
public class PosHeldOrderCheckoutRequest {

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    @Pattern(
            regexp = "^(CASH|VNPAY|MIXED)$",
            message = "Phương thức thanh toán chỉ được là CASH, VNPAY hoặc MIXED"
    )
    private String paymentMethod;

    @DecimalMin(value = "0.00", message = "Tiền mặt không được âm")
    private BigDecimal cashGiven;

    @DecimalMin(value = "0.00", message = "Tiền chuyển khoản không được âm")
    private BigDecimal transferAmount;

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod == null
                ? null
                : paymentMethod.trim().toUpperCase(Locale.ROOT);
    }
}
