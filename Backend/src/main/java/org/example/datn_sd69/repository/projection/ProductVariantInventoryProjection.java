package org.example.datn_sd69.repository.projection;

public interface ProductVariantInventoryProjection {

    Long getTotalQuantity();

    Long getSellableQuantity();
}