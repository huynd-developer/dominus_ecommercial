package org.example.datn_sd69.repository.projection;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface InventoryLotViewProjection {
    Integer getId();
    Integer getProductVariantId();
    String getSku();
    String getProductName();
    String getLotCode();
    LocalDate getManufacturedDate();
    LocalDate getReceivedDate();
    LocalDate getExpirationDate();
    Integer getDaysToExpiry();
    Integer getInitialQuantity();
    Integer getQuantityOnHand();
    Integer getSellableQuantity();
    Boolean getIsNearExpiry();
    Boolean getIsExpired();
    Integer getCreatedById();
    String getCreatedByName();
    LocalDateTime getCreatedAt();
    Integer getGoodsReceiptItemId();
    Integer getGoodsReceiptId();
    String getReceiptNo();
    Byte getReceiptType();
    Byte getReceiptStatus();
}