package org.example.datn_sd69.modules.voucher.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class VoucherApplyResponse {
    private String code;
    private BigDecimal discountAmount;
    private String message;
}