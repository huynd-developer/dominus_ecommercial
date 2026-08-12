package org.example.datn_sd69.modules.inventory.dto.response;

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
public class InventoryOverviewResponse {

    private Integer productVariantId;

    private String sku;

    private String productName;

    private Long totalQuantity;

    private Long sellableQuantity;

    private Long nearExpiryQuantity;

    private Long expiredQuantity;

    private Long lockedQuantity;
}
