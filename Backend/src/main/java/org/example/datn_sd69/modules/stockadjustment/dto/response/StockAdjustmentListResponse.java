package org.example.datn_sd69.modules.stockadjustment.dto.response;

import lombok.Getter;
import lombok.Setter;
import org.example.datn_sd69.enums.StockAdjustmentStatus;

import java.time.LocalDateTime;

@Getter
@Setter
public class StockAdjustmentListResponse {

    private Integer id;

    private String adjustmentNo;

    private StockAdjustmentStatus status;

    private String statusLabel;

    private Integer totalLots;

    private Integer matchedLots;

    private Integer mismatchLots;

    private Integer increasedLots;

    private Integer decreasedLots;

    private Integer totalIncrease;

    /**
     * Số dương, ví dụ 5 nghĩa là tổng giảm 5.
     */
    private Integer totalDecrease;

    private Integer createdById;

    private String createdByName;

    private LocalDateTime createdAt;

    private LocalDateTime submittedAt;

    private LocalDateTime approvedAt;

    private LocalDateTime rejectedAt;

    private LocalDateTime cancelledAt;
}