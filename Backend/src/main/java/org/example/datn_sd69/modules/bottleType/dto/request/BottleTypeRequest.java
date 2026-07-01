package org.example.datn_sd69.modules.bottleType.dto.request;

import jakarta.validation.constraints.NotBlank;

public class BottleTypeRequest {
    @NotBlank(message = "Tên loại chai không được để trống")
    private String name;

    private Integer status;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
