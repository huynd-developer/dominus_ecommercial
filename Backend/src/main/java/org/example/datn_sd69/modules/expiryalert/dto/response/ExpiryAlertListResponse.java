package org.example.datn_sd69.modules.expiryalert.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ExpiryAlertListResponse {

    private Integer id;

    private Integer productVariantId;

    private String sku;

    private String productName;

    private String imageUrl;

    private String lotCode;

    private Integer quantityOnHand;

    private Integer sellableQuantity;

    private LocalDate expirationDate;

    private Integer daysToExpiry;

    private Boolean isNearExpiry;

    private Boolean isExpired;
}