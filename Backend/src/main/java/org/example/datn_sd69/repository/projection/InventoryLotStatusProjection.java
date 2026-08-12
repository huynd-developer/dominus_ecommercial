package org.example.datn_sd69.repository.projection;

import java.time.LocalDate;

public interface InventoryLotStatusProjection {

    Integer getInventoryLotId();

    Integer getProductVariantId();

    String getSku();

    String getProductName();

    String getLotCode();

    LocalDate getManufacturedDate();

    LocalDate getReceivedDate();

    LocalDate getExpirationDate();

    Integer getInitialQuantity();

    Integer getQuantityOnHand();

    Boolean getLocked();

    String getLockReason();

    Integer getDaysToExpiry();

    Boolean getExpired();

    Boolean getNearExpiry();

    Integer getSellableQuantity();
}