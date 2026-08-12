package org.example.datn_sd69.modules.goodsreceipt.dto.response;

import lombok.*;
import org.example.datn_sd69.enums.GoodsReceiptStatus;
import org.example.datn_sd69.enums.GoodsReceiptType;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceiptDetailResponse {
    private Integer id;
    private String receiptNo;

    private GoodsReceiptType receiptType;
    private String receiptTypeLabel;

    private GoodsReceiptStatus status;
    private String statusLabel;

    private String note;

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

    private Long totalSku;
    private Long totalQuantity;

    private List<GoodsReceiptItemResponse> items;
}
