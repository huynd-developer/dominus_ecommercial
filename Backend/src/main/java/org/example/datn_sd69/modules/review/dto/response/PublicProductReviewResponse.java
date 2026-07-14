package org.example.datn_sd69.modules.review.dto.response;

import java.time.LocalDateTime;

public record PublicProductReviewResponse(
        Integer reviewId,
        Integer productId,
        Integer productVariantId,
        String productName,
        String brandName,
        String sku,

        String customerName,
        Integer rating,
        String comment,
        LocalDateTime createdAt
) {
}