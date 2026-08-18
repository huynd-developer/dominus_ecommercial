package org.example.datn_sd69.modules.goodsreceipt.dto.response;

import lombok.*;
import org.example.datn_sd69.enums.GoodsReceiptStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceiptApprovalHistoryResponse {
    private Integer id;
    private GoodsReceiptStatus fromStatus;
    private String fromStatusLabel;
    private GoodsReceiptStatus toStatus;
    private String toStatusLabel;
    private Integer actionById;
    private String actionByName;
    private String reason;
    private LocalDateTime actionAt;
}
