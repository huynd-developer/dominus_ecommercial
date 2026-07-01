package org.example.datn_sd69.modules.concentration.dto.request;

import jakarta.validation.constraints.NotBlank;

public class ConcentrationRequest {

    @NotBlank(message = "Tên nồng độ không được để trống")
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