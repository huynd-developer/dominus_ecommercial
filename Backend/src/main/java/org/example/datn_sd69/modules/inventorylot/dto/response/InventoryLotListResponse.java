package org.example.datn_sd69.modules.inventorylot.dto.response;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLotListResponse {
    private Integer id;
    private Integer productVariantId;
    private String sku;
    private String productName;
    private String lotCode;
    private LocalDate receivedDate;
    private LocalDate expirationDate;
    private Integer daysToExpiry;
    private Integer initialQuantity;
    private Integer quantityOnHand;
    private Integer sellableQuantity;
    private Boolean isNearExpiry;
    private Boolean isExpired;
    private Boolean isLocked;
    private String lockReason;
    private Integer goodsReceiptId;
    private String receiptNo;
}
