package org.example.datn_sd69.repository.projection;

import java.time.LocalDateTime;

public interface StockMovementViewProjection {

    Integer getId();

    Integer getInventoryLotId();

    Integer getProductVariantId();

    String getSku();

    String getProductName();

    String getLotCode();

    Byte getMovementType();

    Integer getQuantityChange();

    Integer getQuantityBefore();

    Integer getQuantityAfter();

    String getReferenceType();

    Integer getReferenceId();

    Integer getReferenceLineId();

    String getReason();

    Integer getCreatedById();

    String getCreatedByName();

    LocalDateTime getCreatedAt();
}