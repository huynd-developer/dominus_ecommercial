package org.example.datn_sd69.modules.inventorylot.dto.response;

import lombok.*;
import org.example.datn_sd69.enums.GoodsReceiptStatus;
import org.example.datn_sd69.enums.GoodsReceiptType;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLotSourceResponse {
    private Integer inventoryLotId;
    private Integer goodsReceiptItemId;
    private Integer goodsReceiptId;
    private String receiptNo;
    private GoodsReceiptType receiptType;
    private String receiptTypeLabel;
    private GoodsReceiptStatus receiptStatus;
    private String receiptStatusLabel;
}
