package org.example.datn_sd69.modules.pos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class PosCheckoutRequest {

    // "CASH" hoặc "VNPAY"
    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod;

    // SĐT khách — null nếu khách vãng lai hoàn toàn
    private String customerPhone;

    // BỔ SUNG MỚI: Tên khách hàng (Thu ngân điền tên khách vãng lai hoặc điền để tạo mới thành viên)
    private String customerName;

    // Mã voucher — null nếu không áp dụng
    private String voucherCode;

    // Danh sách sản phẩm
    @Valid
    @NotEmpty(message = "Hóa đơn phải có ít nhất 1 sản phẩm")
    private List<PosItemRequest> items;
}