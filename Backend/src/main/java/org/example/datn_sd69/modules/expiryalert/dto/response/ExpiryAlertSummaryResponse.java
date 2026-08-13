package org.example.datn_sd69.modules.expiryalert.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExpiryAlertSummaryResponse {

    private Integer warningDays;

    private Long nearExpiryLotCount;

    private Long nearExpiryQuantity;

    private Long expiredLotCount;

    private Long expiredQuantity;

    private Long lockedLotCount;

    private Long lockedQuantity;
}