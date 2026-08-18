package org.example.datn_sd69.repository.projection;

public interface ExpiryAlertSummaryProjection {

    Integer getWarningDays();

    Long getNearExpiryLotCount();

    Long getNearExpiryQuantity();

    Long getExpiredLotCount();

    Long getExpiredQuantity();
}