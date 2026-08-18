package org.example.datn_sd69.repository.projection;

import java.time.LocalDateTime;

public interface GoodsReceiptListProjection {
    Integer getId();
    String getReceiptNo();
    Byte getReceiptType();
    Byte getStatus();
    String getNote();
    Integer getCreatedById();
    String getCreatedByName();
    LocalDateTime getCreatedAt();
    LocalDateTime getSubmittedAt();
    LocalDateTime getApprovedAt();
    LocalDateTime getRejectedAt();
    LocalDateTime getCancelledAt();
    Long getTotalSku();
    Long getTotalQuantity();
}
