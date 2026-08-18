package org.example.datn_sd69.modules.pos.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ProductVariantPosResponse {

    private Integer variantId;
    private String sku;

    private String productName;
    private String brandName;

    private String capacityLabel;
    private String bottleTypeName;

    private BigDecimal price;

    /**
     * Compatibility cho FE POS hiện tại.
     * Giá trị thực tế được map từ InventoryLot sellable quantity.
     */
    private Integer stockQuantity;

    /**
     * Tồn có thể bán thật của SKU từ InventoryLot.
     */
    private Integer sellableQuantity;

    /**
     * Compatibility: NSX của lot FEFO tiếp theo nếu có.
     */
    private LocalDate manufacturingDate;

    /**
     * Compatibility: HSD của lot FEFO tiếp theo nếu có.
     */
    private LocalDate expirationDate;

    private Integer status;

    /**
     * true khi không còn lot bán được nhưng vẫn còn tồn ở lot đã hết hạn.
     */
    private Boolean expired;

    private Boolean sellable;
    private String unavailableReason;

    private String imageUrl;
}
