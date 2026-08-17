package org.example.datn_sd69.modules.stockmovement.dto.response;

import lombok.Builder;
import lombok.Getter;

import org.example.datn_sd69.enums.StockMovementType;

import java.time.LocalDateTime;

@Getter
@Builder
public class StockMovementDetailResponse {

    private Integer id;

    private LocalDateTime createdAt;


    private Integer inventoryLotId;

    private Integer productVariantId;

    private String sku;

    private String productName;

    /*
     * Ảnh đại diện sản phẩm.
     *
     * Ưu tiên ảnh IsPrimary = true.
     * Nếu không có ảnh primary thì lấy ảnh đầu tiên.
     */
    private String imageUrl;

    private String lotCode;


    private StockMovementType movementType;

    private String movementTypeLabel;


    private Integer quantityChange;

    private Integer quantityBefore;

    private Integer quantityAfter;


    private String referenceType;

    private Integer referenceId;

    private Integer referenceLineId;


    private String reason;


    private Integer createdById;

    private String createdByName;
}