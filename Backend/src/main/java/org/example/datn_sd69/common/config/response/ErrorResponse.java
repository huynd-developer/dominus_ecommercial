package org.example.datn_sd69.common.config.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorResponse {
    // Mã trạng thái (VD: 400, 401, 500...)
    private int status;

    // Thông báo lỗi chi tiết (VD: "Sai mật khẩu")
    private String message;

    // Dữ liệu đi kèm (Thường để null khi có lỗi)
    private Object data;
}