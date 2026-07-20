package org.example.datn_sd69.modules.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderRequest {

    @NotBlank(message = "Tên người nhận không được để trống")
    @Size(min = 2, max = 100, message = "Tên người nhận phải từ 2 đến 100 ký tự")
    @Pattern(
            regexp = "^(?!.*\\s{2,})[\\p{L}]+(?:\\s[\\p{L}]+)*$",
            message = "Tên người nhận chỉ được chứa chữ, không có số/ký tự đặc biệt và không có khoảng trắng thừa"
    )
    private String customerName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(
            regexp = "^0\\d{9}$",
            message = "Số điện thoại phải gồm đúng 10 số và bắt đầu bằng 0"
    )
    private String customerPhone;

    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    @Size(min = 5, max = 500, message = "Địa chỉ giao hàng phải từ 5 đến 500 ký tự")
    @Pattern(
            regexp = "^(?!.*\\s{2,})[\\p{L}\\d][\\p{L}\\d\\s,./#()\\-]*[\\p{L}\\d)]$",
            message = "Địa chỉ chỉ được chứa chữ, số, khoảng trắng và các ký tự , . / # ( ) -, không có khoảng trắng thừa"
    )
    private String shippingAddress;

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    @Pattern(
            regexp = "(?i)^(COD|VNPAY)$",
            message = "Phương thức thanh toán chỉ hỗ trợ COD hoặc VNPAY"
    )
    private String paymentMethod;

    @Size(max = 255, message = "Ghi chú tối đa 255 ký tự")
    @Pattern(
            regexp = "^[\\p{L}\\d\\s,./#()\\-:;!?]*$",
            message = "Ghi chú chỉ được chứa chữ, số, khoảng trắng và các ký tự , . / # ( ) - : ; ! ?"
    )
    private String note;

    @Size(max = 50, message = "Mã giảm giá tối đa 50 ký tự")
    @Pattern(
            regexp = "^[A-Za-z0-9_-]*$",
            message = "Mã giảm giá chỉ được chứa chữ, số, dấu gạch ngang hoặc gạch dưới"
    )
    private String voucherCode;
}