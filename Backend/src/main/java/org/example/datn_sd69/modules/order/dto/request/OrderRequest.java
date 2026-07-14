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
            regexp = "^[\\p{L}\\s]+$",
            message = "Tên người nhận chỉ được chứa chữ và khoảng trắng"
    )
    private String customerName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(
            regexp = "^0\\d{9}$",
            message = "Số điện thoại phải gồm 10 số và bắt đầu bằng 0"
    )
    private String customerPhone;

    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    @Size(min = 5, max = 500, message = "Địa chỉ giao hàng phải từ 5 đến 500 ký tự")
    @Pattern(
            regexp = "^[\\p{L}\\d\\s,./#()\\-]+$",
            message = "Địa chỉ chỉ được chứa chữ, số, khoảng trắng và các ký tự , . / # ( ) -"
    )
    private String shippingAddress;

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    @Pattern(regexp = "^(COD|VIETQR)$", message = "Phương thức thanh toán không hợp lệ")
    private String paymentMethod;

    @Size(max = 255, message = "Ghi chú tối đa 255 ký tự")
    private String note;

    private String voucherCode;

}