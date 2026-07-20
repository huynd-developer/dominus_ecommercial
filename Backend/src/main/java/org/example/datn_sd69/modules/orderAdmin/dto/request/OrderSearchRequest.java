package org.example.datn_sd69.modules.orderAdmin.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderSearchRequest {

    private String keyword;

    @Min(value = 0, message = "Trạng thái đơn hàng thấp nhất là 0")
    @Max(value = 7, message = "Trạng thái đơn hàng cao nhất là 7")
    private Integer status;

    @Pattern(regexp = "^(ONLINE|IN_STORE)$", message = "Loại đơn hàng bắt buộc phải là 'ONLINE' hoặc 'IN_STORE'")
    private String orderType;

    @Min(value = 0, message = "Số trang (page) không được nhỏ hơn 0")
    private int page = 0;

    @Min(value = 1, message = "Kích thước trang (size) phải lớn hơn hoặc bằng 1")
    private int size = 10;
}