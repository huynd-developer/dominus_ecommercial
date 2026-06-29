package org.example.datn_sd69.modules.cart.dto.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CartItemResponse {
    private Integer cartItemId;
    private Integer productVariantId;
    private String sku;
    private Integer quantity;
    private BigDecimal price; // Đáp ứng task: Query chính xác Price
    private Integer stockQuantity; // Đáp ứng task: Query chính xác StockQuantity
    // Có thể thêm productName, imageUrl... tùy UI cần
}
