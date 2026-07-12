package org.example.datn_sd69.modules.favorite.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FavoriteResponse(
        Integer favoriteId,
        Integer productVariantId,
        Integer productId,
        String productName,
        String brandName,
        String sku,
        BigDecimal price,
        Integer stockQuantity,
        Double capacityValue,
        String bottleTypeName,
        String imageUrl,
        LocalDateTime createdAt
) {
}