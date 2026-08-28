package org.example.datn_sd69.modules.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CancelOrderRequest {

    private static final String ALLOWED_CANCEL_REASON_REGEX =
            "^(Muốn thay đổi địa chỉ nhận hàng|Muốn thay đổi số điện thoại nhận hàng|Muốn thay đổi sản phẩm hoặc phân loại|Muốn thay đổi số lượng sản phẩm|Muốn thay đổi phương thức thanh toán|Quên áp dụng mã giảm giá|Đặt nhầm sản phẩm|Không còn nhu cầu mua nữa|Tìm thấy sản phẩm phù hợp hơn|Khác)$";

    @NotBlank(message = "Vui lòng chọn lý do hủy đơn")
    @Size(max = 255, message = "Lý do hủy đơn không được vượt quá 255 ký tự")
    @Pattern(
            regexp = ALLOWED_CANCEL_REASON_REGEX,
            message = "Lý do hủy đơn không hợp lệ"
    )
    private String cancelReason;

    public void setCancelReason(String cancelReason) {
        if (cancelReason == null) {
            this.cancelReason = null;
            return;
        }

        this.cancelReason = cancelReason
                .trim()
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s{2,}", " ");
    }
}