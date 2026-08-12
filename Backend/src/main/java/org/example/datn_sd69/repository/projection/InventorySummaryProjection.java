package org.example.datn_sd69.repository.projection;

public interface InventorySummaryProjection {

    Long getTotalSku();

    Long getInStockSku();

    Long getOutOfStockSku();

    Long getTotalQuantity();

    Long getSellableQuantity();

    Long getNearExpiryQuantity();

    Long getExpiredQuantity();

    Long getLockedQuantity();
}