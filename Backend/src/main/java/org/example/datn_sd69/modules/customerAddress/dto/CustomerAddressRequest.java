package org.example.datn_sd69.modules.customerAddress.dto;

import lombok.Data;

@Data
public class CustomerAddressRequest {
    private String recipientName;
    private String phone;
    private String provinceCode;
    private String provinceName;
    private String wardCode;
    private String wardName;
    private String specificAddress;
    private Boolean isDefault;
}