package org.example.datn_sd69.modules.capacity.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CapacityRequest {

    @NotNull(message = "Dung tích không được để trống")
    @Positive(message = "Dung tích phải lớn hơn 0")
    private Double value;

    private Integer status;

    // --- THÊM GETTER VÀ SETTER THỦ CÔNG VÀO ĐÂY LÀ HẾT LỖI ---

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}