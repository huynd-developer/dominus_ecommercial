package org.example.datn_sd69.modules.orderAdmin.dto.request;

import lombok.Data;

@Data
public class OrderRequestDTO {

    private String keyword;

    private Integer status;

    private String orderType;

    private Integer page = 0;

    private Integer size = 10;
}