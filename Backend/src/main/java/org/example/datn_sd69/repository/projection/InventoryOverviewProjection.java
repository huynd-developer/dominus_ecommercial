package org.example.datn_sd69.repository.projection;

public interface InventoryOverviewProjection {

    Integer getProductVariantId();

    String getSku();

    String getProductName();

    Long getTotalQuantity();

    Long getSellableQuantity();

    Long getNearExpiryQuantity();

    Long getExpiredQuantity();

    Long getLockedQuantity();
}
