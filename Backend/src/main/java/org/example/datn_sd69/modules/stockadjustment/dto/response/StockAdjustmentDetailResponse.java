package org.example.datn_sd69.modules.stockadjustment.dto.response;

import lombok.Getter;
import lombok.Setter;
import org.example.datn_sd69.enums.StockAdjustmentStatus;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class StockAdjustmentDetailResponse {

    private Integer id;

    private String adjustmentNo;

    private StockAdjustmentStatus status;

    private String statusLabel;

    private String note;

    private Integer totalLots;

    private Integer matchedLots;

    private Integer mismatchLots;

    private Integer increasedLots;

    private Integer decreasedLots;

    private Integer totalIncrease;

    private Integer totalDecrease;

    private Integer createdById;

    private String createdByName;

    private LocalDateTime createdAt;

    private Integer submittedById;

    private String submittedByName;

    private LocalDateTime submittedAt;

    private Integer approvedById;

    private String approvedByName;

    private LocalDateTime approvedAt;

    private Integer rejectedById;

    private String rejectedByName;

    private LocalDateTime rejectedAt;

    private String rejectionReason;

    private Integer cancelledById;

    private String cancelledByName;

    private LocalDateTime cancelledAt;

    private String cancellationReason;

    private List<StockAdjustmentItemResponse> items;
}