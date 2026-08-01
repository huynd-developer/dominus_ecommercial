package org.example.datn_sd69.modules.customerOrder.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SubmitDeliveryRefundBankRequest(

        @NotBlank(message = "Vui lòng chọn ngân hàng")
        @Size(min = 2, max = 100, message = "Tên ngân hàng phải từ 2 đến 100 ký tự")
        @Pattern(
                regexp = "^[\\p{L}0-9 .()\\-/&]+$",
                message = "Tên ngân hàng chứa ký tự không hợp lệ"
        )
        String bankName,

        @NotBlank(message = "Vui lòng nhập số tài khoản")
        @Size(min = 6, max = 50, message = "Số tài khoản phải từ 6 đến 50 ký tự")
        @Pattern(
                regexp = "^[0-9 ]+$",
                message = "Số tài khoản chỉ được chứa số và khoảng trắng"
        )
        String bankAccountNumber,

        @NotBlank(message = "Vui lòng nhập tên chủ tài khoản")
        @Size(min = 2, max = 100, message = "Tên chủ tài khoản phải từ 2 đến 100 ký tự")
        @Pattern(
                regexp = "^[\\p{L} .'-]+$",
                message = "Tên chủ tài khoản chỉ được chứa chữ cái, khoảng trắng, dấu chấm, dấu nháy hoặc dấu gạch ngang"
        )
        String bankAccountHolder
) {
}