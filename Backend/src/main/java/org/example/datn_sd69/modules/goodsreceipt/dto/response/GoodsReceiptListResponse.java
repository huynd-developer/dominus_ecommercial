package org.example.datn_sd69.modules.goodsreceipt.dto.response;

import lombok.*;
import org.example.datn_sd69.enums.GoodsReceiptStatus;
import org.example.datn_sd69.enums.GoodsReceiptType;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceiptListResponse {
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
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime cancelledAt;
    private Long totalSku;
    private Long totalQuantity;
}
