package org.example.datn_sd69.modules.inventorylot.dto.response;

import lombok.*;
import org.example.datn_sd69.enums.GoodsReceiptStatus;
import org.example.datn_sd69.enums.GoodsReceiptType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLotDetailResponse {
    private Integer id;

    private Integer productVariantId;
    private String sku;
    private String productName;
    private String imageUrl;
    private String lotCode;

    private LocalDate manufacturedDate;
    private LocalDate receivedDate;
    private LocalDate expirationDate;
    private Integer daysToExpiry;

    private Integer initialQuantity;
    private Integer quantityOnHand;
    private Integer sellableQuantity;
    private BigDecimal unitCost;

    private Boolean isNearExpiry;
    private Boolean isExpired;

    private Integer createdById;
    private String createdByName;
    private LocalDateTime createdAt;

    private Integer goodsReceiptItemId;
    private Integer goodsReceiptId;
    private String receiptNo;
    private GoodsReceiptType receiptType;
    private String receiptTypeLabel;
    private GoodsReceiptStatus receiptStatus;
    private String receiptStatusLabel;
}