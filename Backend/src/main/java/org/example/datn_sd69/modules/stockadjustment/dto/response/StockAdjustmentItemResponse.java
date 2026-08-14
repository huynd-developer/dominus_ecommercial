package org.example.datn_sd69.modules.stockadjustment.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockAdjustmentItemResponse {

    private Integer id;

    private Integer inventoryLotId;

    private Integer productVariantId;

    private String sku;

    private String productName;

    private String lotCode;

    /**
     * Snapshot tồn lúc kiểm kê.
     */
    private Integer systemQuantity;

    /**
     * Tồn thực tế đã nhập.
     */
    private Integer actualQuantity;

    /**
     * Không lưu DB.
     * Tính bằng actualQuantity - systemQuantity.
     */
    private Integer quantityDifference;

    /**
     * Tồn hiện tại của lô khi đọc response.
     */
    private Integer currentQuantity;

    private String resultLabel;

    private String reason;
}
