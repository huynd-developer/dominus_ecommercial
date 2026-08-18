package org.example.datn_sd69.repository.projection;

import java.time.LocalDate;

public interface ExpiryAlertViewProjection {

    Integer getId();

    Integer getProductVariantId();

    String getSku();

    String getProductName();

    String getImageUrl();

    String getLotCode();

    Integer getQuantityOnHand();

    Integer getSellableQuantity();

    LocalDate getExpirationDate();

    Integer getDaysToExpiry();

    Boolean getIsNearExpiry();

    Boolean getIsExpired();
}