package org.example.datn_sd69.modules.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventorySummaryResponse {

    private Long totalSku;

    private Long inStockSku;

    private Long outOfStockSku;

    private Long totalQuantity;

    private Long sellableQuantity;

    private Long nearExpiryQuantity;

    private Long expiredQuantity;
}