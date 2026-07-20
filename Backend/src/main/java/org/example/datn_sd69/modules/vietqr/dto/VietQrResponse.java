package org.example.datn_sd69.modules.vietqr.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class VietQrResponse {

    private Integer orderId;

    private String bankCode;
    private String bankBin;
    private String accountNo;
    private String accountName;

    private BigDecimal amount;

    private String transferContent;
    private String qrImageUrl;
}