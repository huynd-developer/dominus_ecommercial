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

    private LocalDateTime promotionEndDate;

    private Integer stockQuantity;

    private String note;

    private String imageUrl;

    private String thumbnailUrl;

    private LocalDate manufacturingDate;

    private LocalDate expirationDate;

    private Integer variantStatus;

    private Boolean expired;

    /**
     * Field cũ, giữ để FE cũ không lỗi.
     */
    private Boolean available;

    /**
     * Field mới đồng bộ với POS/Product/Promotion.
     */
    private Boolean sellable;

    private String unavailableReason;
}