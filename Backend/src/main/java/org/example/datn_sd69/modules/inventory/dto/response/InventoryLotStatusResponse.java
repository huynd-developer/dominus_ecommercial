package org.example.datn_sd69.modules.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLotStatusResponse {

    private Integer inventoryLotId;

    private Integer productVariantId;

    private String sku;

    private String productName;

    private String lotCode;

    private LocalDate manufacturedDate;

    private LocalDate receivedDate;

    private LocalDate expirationDate;

    private Integer initialQuantity;

    private Integer quantityOnHand;

    private Integer sellableQuantity;

    private Integer daysToExpiry;

    private Boolean nearExpiry;

    private Boolean expired;
}