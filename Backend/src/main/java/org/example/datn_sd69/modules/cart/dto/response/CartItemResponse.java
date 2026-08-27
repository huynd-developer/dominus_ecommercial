package org.example.datn_sd69.modules.cart.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CartItemResponse {

    private Integer cartItemId;

    private Integer productVariantId;

    private String sku;

    private String productName;

    private String capacity;

    private String bottleType;

    private Integer quantity;

    private String imageUrl;

    private Integer productId;

    /**
     * Giá thực tế dùng để tính tiền.
     * Nếu đang có Flash Sale: price = salePrice.
     * Nếu không có Flash Sale: price = originalPrice.
     */
    private BigDecimal price;

    /**
     * Giá gốc của biến thể.
     */
    private BigDecimal originalPrice;

    /**
     * Giá sau khi áp dụng Flash Sale.
     */
    private BigDecimal salePrice;

    /**
     * % giảm giá Flash Sale.
     */
    private Double discountPercent;

    private Boolean hasPromotion;

    private Integer promotionId;

    private String promotionName;

    private LocalDateTime promotionStartDate;

    private LocalDateTime promotionEndDate;

    /**
     * LEGACY compatibility.
     * CartService map field này = sellableQuantity từ InventoryLot.
     */
    private Integer stockQuantity;

    /**
     * Tồn có thể bán thật của SKU từ InventoryLot.
     */
    private Integer sellableQuantity;

    private String note;

    private String thumbnailUrl;

    /**
     * Compatibility: ngày của lot FEFO bán tiếp theo nếu có.
     * Không dùng làm business rule ở Cart.
     */
    private LocalDate manufacturingDate;

    /**
     * Compatibility: HSD của lot FEFO bán tiếp theo nếu có.
     * Không dùng làm business rule ở Cart.
     */
    private LocalDate expirationDate;

    private Integer variantStatus;

    /**
     * Compatibility:
     * true khi không còn lot bán được nhưng vẫn còn tồn ở lot đã hết hạn.
     */
    private Boolean expired;

    /**
     * Field cũ, giữ để FE cũ không lỗi.
     */
    private Boolean available;

    /**
     * Đồng bộ với POS/Product/Checkout.
     */
    private Boolean sellable;

    private String unavailableReason;
}