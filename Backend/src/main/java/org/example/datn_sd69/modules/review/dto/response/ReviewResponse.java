package org.example.datn_sd69.modules.review.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewResponse(
        Integer reviewId,
        Integer orderItemId,
        Integer orderId,

        Integer productVariantId,
        Integer productId,
        String productName,
        String brandName,
        String sku,
        String image,

        Integer rating,
        String comment,
        LocalDateTime createdAt,

        Integer approvalStatus,
        String approvalStatusText,
        LocalDateTime approvedAt,
        LocalDateTime rejectedAt,
        String rejectedReason,

        List<String> mediaUrls
) {
}